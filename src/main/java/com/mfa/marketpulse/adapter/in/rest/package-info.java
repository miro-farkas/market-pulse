/**
 * REST and SSE resources plus their DTOs (records). Resources call inbound ports, never outbound adapters.
 *
 * <p>Endpoints return {@code Uni}/{@code Multi}. Streams use SSE with
 * {@code @RestStreamElementType(MediaType.APPLICATION_JSON)}, are bounded per client and release resources on
 * cancellation. Errors are mapped to RFC 7807 {@code application/problem+json}; stack traces never leak.
 */
package com.mfa.marketpulse.adapter.in.rest;
