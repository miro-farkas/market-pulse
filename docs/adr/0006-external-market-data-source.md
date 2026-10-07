# ADR-0006: Binance public market-data WebSocket as the external system

- Status: Accepted
- Date: 2026-10-02

## Context
The demo needs a real, free, high-frequency push stream without credentials.

## Decision
- Use Binance public market-data combined streams (`<symbol>@trade` or `@ticker`), base URL configurable
  (`market-pulse.exchange.ws-url`; default `wss://data-stream.binance.vision`, market-data-only endpoint).
- Client: Quarkus WebSockets Next client in `adapter.out.exchange`, mapping to domain `Tick`.
- Expect disconnects (server closes connections periodically): reconnect with backoff + jitter, resubscribe,
  expose state in health check and metrics.
- Exchange-specific payloads never leave `adapter.out.exchange`.

## Consequences
+ Realistic stream rates; demonstrates backpressure and resilience.
− Availability depends on a third party and region restrictions; a `simulator` profile generating
  synthetic ticks is required for offline demos and tests.

## Alternatives considered
- Coinbase Advanced Trade public WebSocket: viable; similar adapter, kept as fallback.
- OpenSky / Open-Meteo: polling, low frequency – weaker reactive showcase.
