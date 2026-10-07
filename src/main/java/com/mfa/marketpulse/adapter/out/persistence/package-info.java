/**
 * Persistence adapter: Hibernate Reactive Panache entities and repositories, plus reactive PG client cursor
 * queries for large reads (ADR-0003). Entities never leave this package.
 *
 * <p>No {@code java.sql}; schema changes only via Flyway migrations.
 */
package com.mfa.marketpulse.adapter.out.persistence;
