/**
 * Domain model: records, value objects and pure domain logic (e.g. candle aggregation, alert evaluation).
 *
 * <p>Plain Java only. No Quarkus, Mutiny, Jakarta, Jackson, Hibernate or Kafka types. Prices and quantities are
 * {@link java.math.BigDecimal}, time is {@link java.time.Instant} (UTC). Everything here is synchronously unit
 * testable without a container (ADR-0001).
 */
package com.mfa.marketpulse.domain;
