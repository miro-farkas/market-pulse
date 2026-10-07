# ADR-0003: Hibernate Reactive Panache for CRUD, reactive PG client for streaming reads

- Status: Accepted
- Date: 2026-10-02

## Context
Reference data (portfolios, holdings, alert rules) is classic CRUD. Historical candles must be streamed
to clients with backpressure. Hibernate Reactive returns `Uni<List<T>>`, i.e. materializes results.

## Decision
- CRUD via Hibernate Reactive with Panache **repositories** (not active-record entities) in
  `adapter.out.persistence`; entities never leave that package.
- Transaction boundaries in application services via `@WithTransaction`.
- Large reads (candle history) via the reactive PG client with a server-side cursor
  (`PreparedStatement.createStream(fetchSize)` → `Multi`), giving real backpressure from DB to client.
- Schema managed by Flyway (JDBC driver used by Flyway only; enforced by "no java.sql" rule for code).

## Consequences
+ True streaming from DB to SSE client.
+ Panache repositories keep domain/entities separate.
− Two DB access styles; documented here and limited to the candle history query.
− Hibernate Reactive requires a Vert.x context: tests use `@RunOnVertxContext` + `UniAsserter`.

## Alternatives considered
- Only Hibernate Reactive: no true streaming for large result sets.
- Only reactive SQL client: more boilerplate for CRUD.
