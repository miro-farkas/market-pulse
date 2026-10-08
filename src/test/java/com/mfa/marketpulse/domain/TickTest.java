package com.mfa.marketpulse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TickTest {

    private static final Symbol BTC = new Symbol("BTCUSDT");
    private static final Instant TIME = Instant.parse("2026-10-07T10:00:00Z");

    @Test
    void keepsExchangeScale() {
        var tick = new Tick(BTC, new BigDecimal("62000.10000000"), new BigDecimal("0.00100000"), TIME);

        assertThat(tick.price().scale()).isEqualTo(8);
        assertThat(tick.quantity()).isEqualByComparingTo("0.001");
    }

    @Test
    void rejectsNonPositivePrice() {
        assertThatThrownBy(() -> new Tick(BTC, BigDecimal.ZERO, BigDecimal.ONE, TIME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price");
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> new Tick(BTC, BigDecimal.ONE, new BigDecimal("-1"), TIME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void rejectsMissingFields() {
        assertThatThrownBy(() -> new Tick(null, BigDecimal.ONE, BigDecimal.ONE, TIME))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Tick(BTC, BigDecimal.ONE, BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }
}
