package com.mfa.marketpulse.adapter.out.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfa.marketpulse.domain.Symbol;
import com.mfa.marketpulse.domain.Tick;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Maps raw Binance WebSocket text frames to ticks. Anything that is not a valid trade (subscription replies, other
 * event types, malformed JSON, invalid values) yields {@link Optional#empty()} so one bad frame never breaks the
 * stream.
 */
@ApplicationScoped
public class BinanceTradeMapper {

    private final ObjectMapper objectMapper;

    public BinanceTradeMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<Tick> map(String frame) {
        try {
            JsonNode node = objectMapper.readTree(frame);
            if (!BinanceTradeMessage.TRADE.equals(node.path("e").asText())) {
                // Subscription replies look like {"result":null,"id":1}
                Log.debugf("Ignoring non-trade Binance frame: %s", frame);
                return Optional.empty();
            }
            BinanceTradeMessage trade = objectMapper.treeToValue(node, BinanceTradeMessage.class);
            return Optional.of(new Tick(
                    Symbol.of(trade.symbol()),
                    trade.price(),
                    trade.quantity(),
                    Instant.ofEpochMilli(trade.tradeTime())));
        } catch (JsonProcessingException | IllegalArgumentException | NullPointerException e) {
            Log.warnf("Skipping invalid Binance frame (%s): %s", e.getMessage(), frame);
            return Optional.empty();
        }
    }

    /** Subscribes to the trade stream of each symbol, e.g. {@code btcusdt@trade}. */
    static String subscribeMessage(Iterable<Symbol> symbols, int requestId) {
        var params = new StringBuilder();
        for (Symbol symbol : symbols) {
            if (!params.isEmpty()) {
                params.append(',');
            }
            // Symbols are [A-Z0-9]+, so no JSON escaping is needed.
            params.append('"').append(symbol.value().toLowerCase(Locale.ROOT)).append("@trade\"");
        }
        return "{\"method\":\"SUBSCRIBE\",\"params\":[" + params + "],\"id\":" + requestId + "}";
    }
}
