package com.mfa.marketpulse.adapter.out.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfa.marketpulse.domain.Symbol;
import com.mfa.marketpulse.domain.Tick;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SimulatedMarketDataSourceTest {

    private static final Symbol BTC = new Symbol("BTCUSDT");
    private static final Symbol ETH = new Symbol("ETHUSDT");
    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");

    private final SimulatedMarketDataSource source = new SimulatedMarketDataSource(
            Duration.ofMillis(5), new BigDecimal("100"), new Random(42), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void emitsValidTicksForEverySymbol() {
        var subscriber = source.ticks(Set.of(BTC, ETH)).subscribe().withSubscriber(AssertSubscriber.create(40));

        subscriber.awaitItems(40);
        subscriber.cancel();

        assertThat(subscriber.getItems()).extracting(Tick::symbol).contains(BTC, ETH);
        assertThat(subscriber.getItems()).allSatisfy(tick -> {
            assertThat(tick.price().scale()).isEqualTo(2);
            assertThat(tick.quantity()).isPositive().isLessThan(BigDecimal.ONE);
            assertThat(tick.tradeTime()).isEqualTo(NOW);
        });
    }

    @Test
    void walkStaysCloseToPreviousPrice() {
        var price = new BigDecimal("100.00");
        for (int i = 0; i < 1_000; i++) {
            var next = source.step(price);
            // 0.1 % volatility: a single step beyond 1 % would be a 10-sigma event
            assertThat(next.subtract(price).abs()).isLessThanOrEqualTo(price.movePointLeft(2));
            price = next;
        }
    }

    @Test
    void priceNeverDropsBelowFloor() {
        assertThat(source.step(new BigDecimal("0.01"))).isGreaterThanOrEqualTo(new BigDecimal("0.01"));
    }
}
