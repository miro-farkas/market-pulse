package com.mfa.marketpulse.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfa.marketpulse.domain.Symbol;
import com.mfa.marketpulse.domain.Tick;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = InMemoryTicksResource.class, restrictToAnnotatedClass = true)
class KafkaTickPublisherTest {

    @Inject
    KafkaTickPublisher publisher;

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void sendsVersionedEventKeyedBySymbol() throws Exception {
        InMemorySink<TickEvent> sink = connector.sink("market-ticks");
        var tick = new Tick(
                new Symbol("TESTUSDT"),
                new BigDecimal("62012.34000000"),
                new BigDecimal("0.00150000"),
                Instant.parse("2026-10-07T10:00:00.120Z"));

        publisher.publish(tick).await().atMost(Duration.ofSeconds(5));

        Message<TickEvent> message = sink.received().stream()
                .filter(m -> m.getPayload().symbol().equals("TESTUSDT"))
                .findFirst()
                .orElseThrow();
        assertThat(message.getMetadata(OutgoingKafkaRecordMetadata.class))
                .hasValueSatisfying(metadata -> assertThat(metadata.getKey()).isEqualTo("TESTUSDT"));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(message.getPayload()));
        assertThat(json.get("eventId").asText()).isNotBlank();
        assertThat(json.get("version").asInt()).isEqualTo(1);
        assertThat(json.get("occurredAt").asText()).isEqualTo("2026-10-07T10:00:00.120Z");
        assertThat(json.get("price").isTextual()).isTrue();
        assertThat(json.get("price").asText()).isEqualTo("62012.34000000");
        assertThat(json.get("quantity").asText()).isEqualTo("0.00150000");
    }

    @Test
    void simulatorTicksAreIngestedForConfiguredSymbols() {
        InMemorySink<TickEvent> sink = connector.sink("market-ticks");

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(sink.received())
                        .extracting(m -> m.getPayload().symbol())
                        .contains("BTCUSDT", "ETHUSDT", "SOLUSDT"));
    }
}
