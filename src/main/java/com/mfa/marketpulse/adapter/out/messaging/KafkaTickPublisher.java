package com.mfa.marketpulse.adapter.out.messaging;

import com.mfa.marketpulse.application.port.out.TickPublisher;
import com.mfa.marketpulse.domain.Tick;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

/** Sends ticks to {@code market.ticks}, keyed by symbol so each symbol's ticks stay in order on one partition. */
@ApplicationScoped
public class KafkaTickPublisher implements TickPublisher {

    private final MutinyEmitter<TickEvent> emitter;

    public KafkaTickPublisher(
            @Channel("market-ticks") @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 1024)
                    MutinyEmitter<TickEvent> emitter) {
        this.emitter = emitter;
    }

    @Override
    public Uni<Void> publish(Tick tick) {
        var metadata = OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(tick.symbol().value())
                .build();
        return emitter.sendMessage(
                Message.of(TickEvent.of(tick, UUID.randomUUID())).addMetadata(metadata));
    }
}
