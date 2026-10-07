/**
 * Use cases. Orchestrate domain logic and talk to the outside world only through ports.
 *
 * <p>May use Mutiny ({@code Uni}/{@code Multi}), CDI and {@code @WithTransaction}. No REST, JPA, Kafka, Vert.x or
 * Jackson types. Never block in a Mutiny pipeline; blocking methods must be marked {@code @Blocking} (ADR-0002).
 */
package com.mfa.marketpulse.application;
