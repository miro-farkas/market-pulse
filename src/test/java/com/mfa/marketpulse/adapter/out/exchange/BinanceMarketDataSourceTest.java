package com.mfa.marketpulse.adapter.out.exchange;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import com.mfa.marketpulse.domain.Symbol;
import com.mfa.marketpulse.domain.Tick;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Binance client against {@link FakeBinanceServer}: subscribe, map, reconnect after the server drops us. */
@QuarkusTest
@TestProfile(BinanceMarketDataSourceTest.FakeBinanceProfile.class)
class BinanceMarketDataSourceTest {

    public static class FakeBinanceProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "market-pulse.source", "binance",
                    "market-pulse.exchange.ws-url", "ws://localhost:${quarkus.http.test-port:8081}/fake-binance",
                    "market-pulse.exchange.reconnect.initial-backoff", "50ms",
                    "market-pulse.exchange.reconnect.max-backoff", "200ms");
        }
    }

    @Inject
    BinanceMarketDataSource source;

    @Test
    void streamsTradesAndReconnectsAfterServerDropsConnection() {
        var subscriber = source.ticks(Set.of(new Symbol("BTCUSDT")))
                .subscribe()
                .withSubscriber(AssertSubscriber.<Tick>create(Long.MAX_VALUE));

        subscriber.awaitItems(FakeBinanceServer.TRADES_PER_SUBSCRIBE, Duration.ofSeconds(10));
        int openedBefore = FakeBinanceServer.OPENED.get();

        FakeBinanceServer.dropAllConnections();

        subscriber.awaitItems(2 * FakeBinanceServer.TRADES_PER_SUBSCRIBE, Duration.ofSeconds(10));
        subscriber.assertNotTerminated();
        assertThat(FakeBinanceServer.OPENED.get()).isGreaterThan(openedBefore);
        assertThat(subscriber.getItems())
                .allSatisfy(tick -> assertThat(tick.symbol()).isEqualTo(new Symbol("BTCUSDT")));
        subscriber.cancel();
    }

    @Test
    void readinessReportsExchangeConnection() {
        // The ingestion service connects at startup.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> given().when()
                        .get("/q/health/ready")
                        .then()
                        .statusCode(200)
                        .body("checks.find { it.name == 'exchange-connection' }.status", equalTo("UP"))
                        .body("checks.find { it.name == 'exchange-connection' }.data.state", equalTo("CONNECTED")));
    }
}
