package com.mfa.marketpulse.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfa.marketpulse.application.port.in.IngestTicksUseCase.Stats;
import com.mfa.marketpulse.application.port.out.MarketDataSource;
import com.mfa.marketpulse.application.port.out.TickPublisher;
import com.mfa.marketpulse.domain.Symbol;
import com.mfa.marketpulse.domain.Tick;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class TickIngestionServiceTest {

    private static final Symbol BTC = new Symbol("BTCUSDT");
    private static final Symbol ETH = new Symbol("ETHUSDT");

    @Test
    void publishesEveryTickWhenPublisherKeepsUp() {
        var publisher = new RecordingPublisher(tick -> Uni.createFrom().voidItem());
        var service = service(symbols -> Multi.createFrom().items(tick(1), tick(2), tick(3)), publisher, 10);

        service.start();

        assertThat(publisher.received).extracting(Tick::price).containsExactly(price(1), price(2), price(3));
        assertThat(service.stats()).isEqualTo(new Stats(false, 3, 0, 0));
    }

    @Test
    void dropsTicksInsteadOfBufferingWhenAllPublishesAreInFlight() {
        // Publishes never complete, so only maxInFlight ticks can be outstanding.
        var publisher = new RecordingPublisher(tick -> Uni.createFrom().nothing());
        var service =
                service(symbols -> Multi.createFrom().range(1, 101).map(TickIngestionServiceTest::tick), publisher, 4);

        service.start();

        assertThat(publisher.received).hasSize(4);
        assertThat(service.stats().dropped()).isEqualTo(96);
        assertThat(service.stats().published()).isZero();
    }

    @Test
    void failedPublishIsCountedAndStreamContinues() {
        var calls = new AtomicInteger();
        var publisher = new RecordingPublisher(tick -> calls.incrementAndGet() == 1
                ? Uni.createFrom().failure(new IllegalStateException("broker down"))
                : Uni.createFrom().voidItem());
        var service = service(symbols -> Multi.createFrom().items(tick(1), tick(2), tick(3)), publisher, 10);

        service.start();

        assertThat(service.stats()).isEqualTo(new Stats(false, 2, 0, 1));
    }

    @Test
    void subscribesOnceForConfiguredSymbolsAndStopCancelsTheSource() {
        var requested = new ArrayList<Set<Symbol>>();
        var cancelled = new AtomicBoolean();
        var service = service(
                symbols -> {
                    requested.add(symbols);
                    return Multi.createFrom().<Tick>nothing().onCancellation().invoke(() -> cancelled.set(true));
                },
                new RecordingPublisher(tick -> Uni.createFrom().voidItem()),
                10);

        service.start();
        service.start();
        assertThat(service.stats().running()).isTrue();

        service.stop();

        assertThat(requested).containsExactly(Set.of(BTC, ETH));
        assertThat(cancelled).isTrue();
        assertThat(service.stats().running()).isFalse();
    }

    @Test
    void sourceFailureStopsIngestion() {
        var service = service(
                symbols -> Multi.createFrom().failure(new IllegalStateException("gone")),
                new RecordingPublisher(tick -> Uni.createFrom().voidItem()),
                10);

        service.start();

        assertThat(service.stats().running()).isFalse();
    }

    private static TickIngestionService service(MarketDataSource source, TickPublisher publisher, int maxInFlight) {
        return new TickIngestionService(source, publisher, Set.of(BTC, ETH), maxInFlight);
    }

    private static Tick tick(int n) {
        return new Tick(
                BTC,
                price(n),
                BigDecimal.ONE,
                Instant.parse("2026-10-07T10:00:00Z").plusSeconds(n));
    }

    private static BigDecimal price(int n) {
        return BigDecimal.valueOf(100 + n);
    }

    private static final class RecordingPublisher implements TickPublisher {

        final List<Tick> received = new CopyOnWriteArrayList<>();
        private final Function<Tick, Uni<Void>> behaviour;

        RecordingPublisher(Function<Tick, Uni<Void>> behaviour) {
            this.behaviour = behaviour;
        }

        @Override
        public Uni<Void> publish(Tick tick) {
            received.add(tick);
            return behaviour.apply(tick);
        }
    }
}
