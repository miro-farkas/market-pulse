package com.mfa.marketpulse.config;

import com.mfa.marketpulse.adapter.out.exchange.BinanceMarketDataSource;
import com.mfa.marketpulse.adapter.out.exchange.SimulatedMarketDataSource;
import com.mfa.marketpulse.application.port.out.MarketDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

/** Selects the {@link MarketDataSource} implementation from {@code market-pulse.source} at runtime. */
@ApplicationScoped
public class MarketDataSourceProducer {

    @Produces
    @ApplicationScoped
    MarketDataSource marketDataSource(
            MarketPulseConfig config,
            Instance<SimulatedMarketDataSource> simulator,
            Instance<BinanceMarketDataSource> binance) {
        return switch (config.source()) {
            case SIMULATOR -> simulator.get();
            case BINANCE -> binance.get();
        };
    }
}
