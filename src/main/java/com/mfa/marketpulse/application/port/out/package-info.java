/**
 * Outbound ports: interfaces the use cases need from the outside world (persistence, event publishing, market
 * data source). Implemented in {@code adapter.out}.
 *
 * <p>Signatures use domain types and Mutiny {@code Uni}/{@code Multi} only.
 */
package com.mfa.marketpulse.application.port.out;
