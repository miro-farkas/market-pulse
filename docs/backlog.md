# Backlog

Work top to bottom. One item = one branch = one PR. Each item is done when its acceptance criteria hold
and `./mvnw verify` is green. Update the status column in the same PR.

| ID | Item | Status |
|---|---|---|
| M0 | Project skeleton | todo |
| M1 | Tick ingestion (simulator + Binance) | todo |
| M2 | Live price SSE stream | todo |
| M3 | Candle aggregation + persistence | todo |
| M4 | Candle history + live SSE (DB cursor stream) | todo |
| M5 | Portfolios, holdings, alert rules CRUD + outbox | todo |
| M6 | Alert evaluation + alert SSE | todo |
| M7 | Portfolio valuation + valuation SSE | todo |
| M8 | Virtual-thread report endpoint | todo |
| M9 | Observability + hardening | todo |

## M0 – Project skeleton
- Package structure per ADR-0001 with `package-info.java` per layer describing its rules.
- `@ConfigMapping` `MarketPulseConfig` (exchange url, symbols, sse max rate, outbox poll interval).
- Flyway `V1__init.sql` with all tables from architecture.md §6.
- RFC 7807 exception mapper, OpenAPI info, health endpoint.
- **AC:** `./mvnw verify` green; `quarkus:dev` starts with Dev Services; `/q/health` UP.

## M1 – Tick ingestion
- Domain `Tick` record + validation. Port `MarketDataSource` (`Multi<Tick> ticks(Set<Symbol>)`).
- Adapters: `SimulatedMarketDataSource` (profile `simulator`, random walk, configurable rate) and
  `BinanceMarketDataSource` (WebSockets Next client, reconnect with backoff + jitter).
- Use case publishes to `market.ticks` (key = symbol) via `@Channel` emitter; bounded buffer, drop on overflow.
- Health check for exchange connection.
- **AC:** ticks visible in Dev UI Kafka; reconnect tested (simulated disconnect); mapping unit-tested
  with recorded Binance JSON samples in `src/test/resources`.

## M2 – Live price SSE
- Per-instance live consumer of `market.ticks` → shared hot `Multi` → `GET /api/prices/stream`.
- Filter by `symbols`, sample to `maxRate` per symbol per client, cancellation releases resources.
- **AC:** test with two clients at different rates; disconnect test shows subscriber count back to 0;
  metric `sse.clients.connected`.

## M3 – Candles
- Pure `CandleAggregator` in domain (event time windows, late ticks ignored) with thorough unit tests.
- Processor `market.ticks` → `market.candles.1m`; persister upserts into `candle`; DLQ configured.
- **AC:** aggregator unit tests cover window boundaries and late ticks; integration test with in-memory
  connector; duplicate candle event doesn't create duplicates.

## M4 – Candle history + live
- Reactive PG client cursor stream (`createStream(fetchSize)`) → `Multi<Candle>`, then concat live candles,
  de-duplicated by `openTime`. `GET /api/candles/{symbol}/stream?from=`.
- **AC:** test with 10k seeded candles proves streaming (bounded memory, first item before last is read);
  no gap/duplicate at history→live switch.

## M5 – Reference data CRUD + outbox
- Portfolios, holdings, alert rules: REST resources, use cases, Panache repositories, validation.
- Outbox insert in same `@WithTransaction`; reactive relay (`FOR UPDATE SKIP LOCKED`) → `reference-data.changes`.
- **AC:** REST tests for CRUD + validation errors (problem+json); test proves outbox row is written in the
  same transaction (rollback = no row); relay publishes exactly the committed changes.

## M6 – Alerts
- Pure `AlertRuleEvaluator` (crossing semantics, percent change window, cooldown) in domain.
- Processor: loads rules from DB as `Multi` at start, applies `reference-data.changes`, evaluates ticks,
  publishes `alerts.triggered`, stores alert history. `GET /api/alerts/stream`.
- **AC:** evaluator unit tests for each rule type incl. cooldown; rule change via REST takes effect without restart.

## M7 – Valuations
- Processor keeps holdings + last prices, emits `portfolio.valuations` max 1/s per portfolio.
  `GET /api/portfolios/{id}/valuation/stream`.
- **AC:** BigDecimal math tested; throttling verified; holding change reflected in next valuation.

## M8 – Virtual-thread report
- `GET /api/reports/portfolios/{id}` with `@RunOnVirtualThread`, blocking REST client to exchange 24h stats.
- Short section in README contrasting it with the Mutiny endpoints.
- **AC:** ArchUnit passes without exceptions; endpoint test green.

## M9 – Observability + hardening
- Micrometer: ticks/s, dropped items, SSE clients, consumer lag; JSON logging in prod profile.
- Timeouts on all outbound calls; graceful shutdown completes SSE streams.
- README: run instructions, curl examples for every stream, architecture diagram link.
