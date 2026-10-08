package com.mfa.marketpulse.config;

import io.smallrye.config.ConfigMapping;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
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

    Ingestion ingestion();

    Simulator simulator();

    enum Source {
        BINANCE,
        SIMULATOR
    }

    interface Exchange {

        @NotBlank String wsUrl();

        /** Trading pairs to subscribe to, uppercase (e.g. {@code BTCUSDT}). */
        @NotEmpty List<@Pattern(regexp = "[A-Z0-9]+", message = "must be an uppercase symbol") String> symbols();

        /**
         * Reconnect when no frame arrives for this long. Catches half-open connections (laptop sleep, NAT timeout)
         * that never report a close.
         */
        Duration staleTimeout();

        Reconnect reconnect();

        interface Reconnect {

            Duration initialBackoff();

            Duration maxBackoff();

            /** Random spread applied to each backoff delay (0 = none, 1 = up to ±100 %). */
            @DecimalMin("0.0") @DecimalMax("1.0") double jitter();
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

    interface Ingestion {

        /** Publishes waiting for Kafka acknowledgement at the same time; further ticks are dropped. */
        @Positive int maxInFlight();
    }

    /** Synthetic random-walk ticks, used when {@code source=simulator} (ADR-0006). */
    interface Simulator {

        /** Ticks per second for each symbol. */
        @Positive int ticksPerSecond();

        /** Starting price for every symbol. */
        @Positive BigDecimal initialPrice();
    }
}
