package com.mfa.marketpulse.adapter.out.exchange;

import com.mfa.marketpulse.config.MarketPulseConfig;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Locale;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/** Ready only while connected to the exchange. The simulator has no connection and is always ready. */
@Readiness
@ApplicationScoped
public class ExchangeConnectionHealthCheck implements HealthCheck {

    static final String NAME = "exchange-connection";

    private final MarketPulseConfig.Source source;
    private final BinanceMarketDataSource binance;

    public ExchangeConnectionHealthCheck(MarketPulseConfig config, BinanceMarketDataSource binance) {
        this.source = config.source();
        this.binance = binance;
    }

    @Override
    public HealthCheckResponse call() {
        var response =
                HealthCheckResponse.named(NAME).withData("source", source.name().toLowerCase(Locale.ROOT));
        if (source == MarketPulseConfig.Source.SIMULATOR) {
            return response.up().build();
        }
        var status = binance.status();
        response.status(status.state() == BinanceMarketDataSource.State.CONNECTED)
                .withData("state", status.state().name())
                .withData("connects", status.connects());
        if (status.lastMessageAt() != null) {
            response.withData("lastMessageAt", status.lastMessageAt().toString());
        }
        return response.build();
    }
}
