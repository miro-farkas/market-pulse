package com.mfa.marketpulse.application;

import com.mfa.marketpulse.application.port.in.IngestTicksUseCase;
import com.mfa.marketpulse.application.port.out.MarketDataSource;
import com.mfa.marketpulse.application.port.out.TickPublisher;
import com.mfa.marketpulse.config.MarketPulseConfig;
import com.mfa.marketpulse.domain.Symbol;
import com.mfa.marketpulse.domain.Tick;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Shutdown;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Streams ticks from the {@link MarketDataSource} to the {@link TickPublisher} for the configured symbols.
 *
 * <p>Bounded end to end: at most {@code max-in-flight} publishes wait for acknowledgement; when they are all busy,
 * new ticks are dropped (and counted) instead of buffered. Ticks are superseded within milliseconds, so dropping is
 * cheaper and safer than unbounded memory growth. A failed publish is counted and skipped; the stream continues.
 */
@ApplicationScoped
public class TickIngestionService implements IngestTicksUseCase {

    private final MarketDataSource source;
    private final TickPublisher publisher;
    private final Set<Symbol> symbols;
    private final int maxInFlight;

    private final AtomicReference<Cancellable> subscription = new AtomicReference<>();
    private final AtomicLong published = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    @Inject
    public TickIngestionService(MarketDataSource source, TickPublisher publisher, MarketPulseConfig config) {
        this(
                source,
                publisher,
                config.exchange().symbols().stream().map(Symbol::new).collect(Collectors.toUnmodifiableSet()),
                config.ingestion().maxInFlight());
    }

    TickIngestionService(MarketDataSource source, TickPublisher publisher, Set<Symbol> symbols, int maxInFlight) {
        this.source = source;
        this.publisher = publisher;
        this.symbols = Set.copyOf(symbols);
        this.maxInFlight = maxInFlight;
    }

    void onStartup(@Observes Startup event) {
        start();
    }

    void onShutdown(@Observes Shutdown event) {
        stop();
    }

    @Override
    public void start() {
        // Reserve the slot first so concurrent start() calls subscribe only once.
        Cancellable placeholder = () -> {};
        if (!subscription.compareAndSet(null, placeholder)) {
            return;
        }
        Log.infof("Starting tick ingestion for %s (max in flight %d)", symbols, maxInFlight);
        Cancellable running = ingest().subscribe()
                .with(
                        ignored -> {},
                        failure -> {
                            Log.error("Tick ingestion stopped: market data source failed", failure);
                            subscription.set(null);
                        },
                        () -> {
                            Log.warn("Tick ingestion stopped: market data source completed");
                            subscription.set(null);
                        });
        if (!subscription.compareAndSet(placeholder, running)) {
            running.cancel(); // stop() was called while we were subscribing
        }
    }

    @Override
    public void stop() {
        Cancellable current = subscription.getAndSet(null);
        if (current != null) {
            current.cancel();
            var stats = stats();
            Log.infof(
                    "Stopped tick ingestion: published %d, dropped %d, failed %d",
                    stats.published(), stats.dropped(), stats.failed());
        }
    }

    @Override
    public Stats stats() {
        return new Stats(subscription.get() != null, published.get(), dropped.get(), failed.get());
    }

    Multi<Void> ingest() {
        return source.ticks(symbols)
                .onOverflow()
                .invoke(tick -> dropped.incrementAndGet())
                .drop()
                .onItem()
                .transformToUni(this::publish)
                .merge(maxInFlight);
    }

    private Uni<Void> publish(Tick tick) {
        return publisher
                .publish(tick)
                .onItem()
                .invoke(published::incrementAndGet)
                .onFailure()
                .invoke(failure -> {
                    failed.incrementAndGet();
                    Log.warnf("Could not publish tick %s: %s", tick, failure.getMessage());
                })
                .onFailure()
                .recoverWithNull();
    }
}
