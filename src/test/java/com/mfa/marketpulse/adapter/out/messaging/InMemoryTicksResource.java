package com.mfa.marketpulse.adapter.out.messaging;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import java.util.Map;

/** Replaces the Kafka connector of {@code market-ticks} with the in-memory connector. */
public class InMemoryTicksResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        return InMemoryConnector.switchOutgoingChannelsToInMemory("market-ticks");
    }

    @Override
    public void stop() {
        InMemoryConnector.clear();
    }
}
