/**
 * Exchange clients (Binance WebSocket via WebSockets Next, REST). Map exchange messages to domain types.
 *
 * <p>Connections reconnect with exponential backoff and jitter ({@code onFailure().retry().withBackOff(...)})
 * (ADR-0006).
 */
package com.mfa.marketpulse.adapter.out.exchange;
