package com.mfa.marketpulse.adapter.out.exchange;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test-only stand-in for the Binance {@code /ws} endpoint. Answers {@code SUBSCRIBE} like Binance, then pushes one
 * malformed frame and a few trades.
 */
@WebSocket(path = "/fake-binance/ws")
public class FakeBinanceServer {

    static final int TRADES_PER_SUBSCRIBE = 3;
    static final AtomicInteger OPENED = new AtomicInteger();

    private static final Set<WebSocketConnection> CONNECTIONS = ConcurrentHashMap.newKeySet();
    private static final AtomicLong TRADE_ID = new AtomicLong();

    @OnOpen
    void open(WebSocketConnection connection) {
        CONNECTIONS.add(connection);
        OPENED.incrementAndGet();
    }

    @OnClose
    void close(WebSocketConnection connection) {
        CONNECTIONS.remove(connection);
    }

    @OnTextMessage
    Uni<Void> message(String message, WebSocketConnection connection) {
        if (!message.contains("\"SUBSCRIBE\"")) {
            return Uni.createFrom().voidItem();
        }
        Uni<Void> replies = connection.sendText("{\"result\":null,\"id\":1}");
        replies = replies.chain(() -> connection.sendText("{broken"));
        for (int i = 0; i < TRADES_PER_SUBSCRIBE; i++) {
            replies = replies.chain(() -> connection.sendText(trade()));
        }
        return replies;
    }

    /** Simulates the exchange dropping every connection (e.g. its periodic 24 h disconnect). */
    static void dropAllConnections() {
        CONNECTIONS.forEach(c -> c.close().subscribe().with(ignored -> {}, failure -> {}));
    }

    private static String trade() {
        long id = TRADE_ID.incrementAndGet();
        return "{\"e\":\"trade\",\"E\":1759831200123,\"s\":\"BTCUSDT\",\"t\":" + id
                + ",\"p\":\"62012.34000000\",\"q\":\"0.00150000\",\"T\":1759831200120,\"m\":true,\"M\":true}";
    }
}
