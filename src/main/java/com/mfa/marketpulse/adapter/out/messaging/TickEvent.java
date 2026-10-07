package com.mfa.marketpulse.adapter.out.messaging;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mfa.marketpulse.domain.Tick;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload of {@code market.ticks} (key = symbol). Prices and quantities are JSON strings so no precision or scale is
 * lost.
 *
 * @param occurredAt exchange trade time
 */
public record TickEvent(
        UUID eventId,
        int version,
        Instant occurredAt,
        String symbol,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal price,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal quantity) {

    public static final int VERSION = 1;

    static TickEvent of(Tick tick, UUID eventId) {
        return new TickEvent(eventId, VERSION, tick.tradeTime(), tick.symbol().value(), tick.price(), tick.quantity());
    }
}
