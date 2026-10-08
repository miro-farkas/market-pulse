package com.mfa.marketpulse.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MarketPulseConfigTest {

    @Inject
    MarketPulseConfig config;

    @Test
    void bindsApplicationProperties() {
        // %test profile: no network access to the exchange in tests
        assertThat(config.source()).isEqualTo(MarketPulseConfig.Source.SIMULATOR);
        assertThat(config.exchange().wsUrl()).isEqualTo("wss://data-stream.binance.vision");
        assertThat(config.exchange().symbols()).containsExactly("BTCUSDT", "ETHUSDT", "SOLUSDT");
        assertThat(config.exchange().staleTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.exchange().reconnect().initialBackoff()).isEqualTo(Duration.ofSeconds(1));
        assertThat(config.exchange().reconnect().maxBackoff()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.exchange().reconnect().jitter()).isEqualTo(0.2);
        assertThat(config.sse().defaultMaxRatePerSecond()).isEqualTo(5);
        assertThat(config.outbox().pollInterval()).isEqualTo(Duration.ofMillis(500));
        assertThat(config.outbox().batchSize()).isEqualTo(100);
        assertThat(config.ingestion().maxInFlight()).isEqualTo(256);
        assertThat(config.simulator().ticksPerSecond()).isEqualTo(5);
        assertThat(config.simulator().initialPrice()).isEqualByComparingTo("100");
    }
}
