package com.mfa.marketpulse.adapter.out.exchange;

import com.mfa.marketpulse.application.port.out.MarketDataSource;
import com.mfa.marketpulse.config.MarketPulseConfig;
import com.mfa.marketpulse.domain.Symbol;
import com.mfa.marketpulse.domain.Tick;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.BasicWebSocketConnector.ExecutionModel;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Binance public trade stream over WebSockets Next (ADR-0006).
 *
 * <p>Each subscription opens one connection to {@code {ws-url}/ws} and sends {@code SUBSCRIBE} for the symbols' trade
 * streams. When the connection closes or fails, the {@code Multi} fails and {@code retry().withBackOff(...)}
 * reconnects and resubscribes. A connection that stays silent for {@code stale-timeout} is treated as lost too,
 * because a half-open TCP connection (e.g. after the host slept) never reports a close. Cancelling the subscription
 * closes the connection.
 */
@ApplicationScoped
@Typed(BinanceMarketDataSource.class)
public class BinanceMarketDataSource implements MarketDataSource {

    public enum State {
        DISCONNECTED,
        CONNECTED
    }

    /**
     * Connection state for the health check. CONNECTED while at least one subscription has an open connection;
     * {@code connects} counts successful connections over the bean's lifetime (reconnects included).
     */
    public record Status(State state, long connects, Instant lastMessageAt) {}

    private final MarketPulseConfig.Exchange config;
    private final BinanceTradeMapper mapper;

    private final AtomicInteger openConnections = new AtomicInteger();
    private final AtomicLong connects = new AtomicLong();
    private final AtomicReference<Instant> lastMessageAt = new AtomicReference<>();

    public BinanceMarketDataSource(MarketPulseConfig config, BinanceTradeMapper mapper) {
        this.config = config.exchange();
        this.mapper = mapper;
    }

    @Override
    public Multi<Tick> ticks(Set<Symbol> symbols) {
        var reconnect = config.reconnect();
        return Multi.createFrom()
                .<String>emitter(emitter -> connect(List.copyOf(symbols), emitter), BackPressureStrategy.DROP)
                // A silent connection is treated like a closed one: fail, close it, reconnect.
                .ifNoItem()
                .after(config.staleTimeout())
                .failWith(() -> new ExchangeConnectionLostException(
                        "no message for " + config.staleTimeout() + ", connection presumed dead"))
                .onFailure()
                .invoke(failure ->
                        Log.warnf("Binance stream lost (%s), reconnecting with backoff", failure.getMessage()))
                .onFailure()
                .retry()
                .withBackOff(reconnect.initialBackoff(), reconnect.maxBackoff())
                .withJitter(reconnect.jitter())
                .indefinitely()
                .onItem()
                .invoke(frame -> lastMessageAt.set(Instant.now()))
                .onItem()
                .transformToIterable(frame -> mapper.map(frame).stream().toList());
    }

    public Status status() {
        var state = openConnections.get() > 0 ? State.CONNECTED : State.DISCONNECTED;
        return new Status(state, connects.get(), lastMessageAt.get());
    }

    /** One connection attempt. The emitter fails when the connection closes, which triggers the retry. */
    private void connect(List<Symbol> symbols, MultiEmitter<? super String> emitter) {
        var connection = new AtomicReference<WebSocketClientConnection>();
        var counted = new AtomicBoolean();
        Runnable release = () -> {
            if (counted.compareAndSet(true, false)) {
                openConnections.decrementAndGet();
            }
        };
        emitter.onTermination(() -> {
            release.run();
            close(connection.get());
        });

        BasicWebSocketConnector.create()
                .baseUri(config.wsUrl())
                .path("/ws")
                .executionModel(ExecutionModel.NON_BLOCKING)
                .onTextMessage((conn, frame) -> emitter.emit(frame))
                .onClose((conn, reason) -> {
                    release.run();
                    failUnlessCancelled(
                            emitter,
                            new ExchangeConnectionLostException("closed by exchange, code " + reason.getCode()));
                })
                .onError((conn, failure) -> failUnlessCancelled(emitter, failure))
                .connect()
                .chain(conn -> {
                    connection.set(conn);
                    if (emitter.isCancelled()) {
                        // Cancelled while connecting: onTermination already ran without a connection to close.
                        return conn.close();
                    }
                    counted.set(true);
                    openConnections.incrementAndGet();
                    connects.incrementAndGet();
                    Log.infof("Connected to Binance %s, subscribing to %s", config.wsUrl(), symbols);
                    return conn.sendText(BinanceTradeMapper.subscribeMessage(symbols, 1));
                })
                .subscribe()
                .with(ignored -> {}, failure -> failUnlessCancelled(emitter, failure));
    }

    /** After cancellation nobody listens; failing would only make Mutiny log a dropped exception. */
    private static void failUnlessCancelled(MultiEmitter<?> emitter, Throwable failure) {
        if (!emitter.isCancelled()) {
            emitter.fail(failure);
        }
    }

    private static void close(WebSocketClientConnection connection) {
        if (connection != null && connection.isOpen()) {
            connection
                    .close()
                    .subscribe()
                    .with(ignored -> {}, failure -> Log.debugf("Closing Binance connection failed: %s", failure));
        }
    }
}
