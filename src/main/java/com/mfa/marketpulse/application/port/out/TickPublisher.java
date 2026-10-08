package com.mfa.marketpulse.application.port.out;

import com.mfa.marketpulse.domain.Tick;
import io.smallrye.mutiny.Uni;

/** Publishes ticks to the rest of the system (the {@code market.ticks} topic). */
public interface TickPublisher {

    /** Completes when the tick is durably accepted, fails if it could not be published. */
    Uni<Void> publish(Tick tick);
}
