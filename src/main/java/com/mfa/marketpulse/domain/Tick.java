package com.mfa.marketpulse.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A single trade reported by the exchange.
 *
 * @param tradeTime exchange event time (authoritative for candles), UTC
 */
public record Tick(Symbol symbol, BigDecimal price, BigDecimal quantity, Instant tradeTime) {

    public Tick {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(tradeTime, "tradeTime");
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("Tick price must be positive: " + price);
        }
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("Tick quantity must be positive: " + quantity);
        }
    }
}
