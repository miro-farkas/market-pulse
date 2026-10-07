package com.mfa.marketpulse.config;

import io.smallrye.config.ConfigMapping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;

/** Application configuration, bound to {@code market-pulse.*}. Validated at startup. */
@ConfigMapping(prefix = "market-pulse")
public interface MarketPulseConfig {

    /** Where ticks come from (ADR-0006). */
    Source source();

    Exchange exchange();

    Sse sse();

    Outbox outbox();

    enum Source {
        BINANCE,
        SIMULATOR
    }

    interface Exchange {

        @NotBlank String wsUrl();

        /** Trading pairs to subscribe to, uppercase (e.g. {@code BTCUSDT}). */
        @NotEmpty List<@Pattern(regexp = "[A-Z0-9]+", message = "must be an uppercase symbol") String> symbols();

        Reconnect reconnect();

        interface Reconnect {

            Duration initialBackoff();

            Duration maxBackoff();
        }
    }

    interface Sse {

        /** Per-client, per-symbol rate limit when the client does not pass {@code maxRate}. */
        @Positive int defaultMaxRatePerSecond();
    }

    interface Outbox {

        Duration pollInterval();

        @Positive int batchSize();
    }
}
