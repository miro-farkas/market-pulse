/**
 * Kafka consumers ({@code @Incoming}) and stream processors. Map versioned event payloads to domain types and
 * call inbound ports.
 *
 * <p>Consumers are idempotent (by {@code eventId}); poison messages go to the DLQ. Live SSE fan-out consumers use a
 * per-instance group id with {@code auto.offset.reset=latest} (ADR-0004).
 */
package com.mfa.marketpulse.adapter.in.messaging;
