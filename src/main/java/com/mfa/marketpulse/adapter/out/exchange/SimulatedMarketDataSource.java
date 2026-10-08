package com.mfa.marketpulse.adapter.out.exchange;

import com.mfa.marketpulse.application.port.out.MarketDataSource;
import com.mfa.marketpulse.config.MarketPulseConfig;
import com.mfa.marketpulse.domain.Symbol;
import com.mfa.marketpulse.domain.Tick;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;

/**
 * Synthetic ticks for offline demos and tests (ADR-0006): an independent random walk per symbol at a fixed rate.
 */
@ApplicationScoped
@Typed(SimulatedMarketDataSource.class)
public class SimulatedMarketDataSource implements MarketDataSource {

    /** Standard deviation of one step, relative to the current price (0.1 %). */
    private static final double VOLATILITY = 0.001;

    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final int PRICE_SCALE = 2;
    private static final int QUANTITY_SCALE = 5;

    private final Duration period;
    private final BigDecimal initialPrice;
    private final RandomGenerator random;
    private final Clock clock;

    @Inject
    public SimulatedMarketDataSource(MarketPulseConfig config) {
        this(
                Duration.ofNanos(1_000_000_000L / config.simulator().ticksPerSecond()),
                config.simulator().initialPrice(),
                new Random(),
                Clock.systemUTC());
    }

    SimulatedMarketDataSource(Duration period, BigDecimal initialPrice, RandomGenerator random, Clock clock) {
        this.period = period;
        this.initialPrice = initialPrice.setScale(PRICE_SCALE, RoundingMode.HALF_EVEN);
        this.random = random;
        this.clock = clock;
    }

    @Override
    public Multi<Tick> ticks(Set<Symbol> symbols) {
        return Multi.createFrom().iterable(symbols).onItem().transformToMultiAndMerge(this::walk);
    }

    private Multi<Tick> walk(Symbol symbol) {
        var lastPrice = new AtomicReference<>(initialPrice);
        return Multi.createFrom()
                .ticks()
                .every(period)
                .onOverflow()
                .drop()
                .map(ignored -> new Tick(symbol, lastPrice.updateAndGet(this::step), quantity(), clock.instant()));
    }

    BigDecimal step(BigDecimal price) {
        var change = price.multiply(BigDecimal.valueOf(random.nextGaussian() * VOLATILITY));
        return price.add(change).max(MIN_PRICE).setScale(PRICE_SCALE, RoundingMode.HALF_EVEN);
    }

    private BigDecimal quantity() {
        // 0.00001 .. 0.99999
        return BigDecimal.valueOf(random.nextLong(1, 100_000), QUANTITY_SCALE);
    }
}
