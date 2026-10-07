# market-pulse

Reactive, event-driven demo backend: live crypto market data (Binance WebSocket) → Kafka →
stream processing (candles, alerts, portfolio valuation) → reactive SSE endpoints.
Purpose is to *demonstrate* reactive + event-driven architecture, so clarity of design beats feature count.

Stack: Java 25, Quarkus 3.40, Mutiny, Quarkus REST (SSE), SmallRye Reactive Messaging (Kafka),
Hibernate Reactive Panache + reactive PG client, Flyway, WebSockets Next (client), Maven.

Read before non-trivial work: `docs/architecture.md`, `docs/domain.md`, `docs/adr/`, `docs/backlog.md`.

## Commands
- Full check (must pass before a task is done): `./mvnw verify`
- Dev mode (Dev Services start Kafka + Postgres, Docker required): `./mvnw quarkus:dev`
- Format: `./mvnw spotless:apply`
- Architecture rules only: `./mvnw test -Dtest=ArchitectureTest`

## Architecture (hexagonal, enforced by ArchUnit in `src/test/.../architecture/ArchitectureTest.java`)
Base package `com.example.marketpulse`:
- `domain..` – records, value objects, pure domain logic (e.g. `CandleAggregator`, `AlertRuleEvaluator`).
  Plain Java only: no Quarkus, Mutiny, Jakarta, Jackson, Hibernate, Kafka.
- `application..` – use cases + ports (`port.in`, `port.out`). May use Mutiny, CDI (`jakarta.enterprise`,
  `jakarta.inject`) and `@WithTransaction`. No REST, JPA, Kafka, Vert.x, Jackson types.
- `adapter.in.rest` (Resources + DTOs), `adapter.in.messaging` (`@Incoming`), `adapter.out.persistence`
  (entities, repositories), `adapter.out.messaging` (`@Outgoing`, `@Channel` emitters),
  `adapter.out.exchange` (Binance WebSocket/REST clients).
- `config..` – `@ConfigMapping` interfaces, producers.
- `adapter.in` never depends on `adapter.out`. Adapters talk through ports.

## Reactive rules (ADR-0002) — most important
- Default is Mutiny: `Uni` for single results, `Multi` for streams. Endpoints return `Uni`/`Multi`.
- NEVER block in a Mutiny pipeline or on an event-loop thread: no `.await()`, `subscribe().asIterable()`,
  `Thread.sleep`, `Future.get/join`, JDBC, blocking clients.
- Blocking code is allowed ONLY in methods annotated `@RunOnVirtualThread` (endpoints/consumers) or
  `@Blocking` (marker on service methods). ArchUnit fails the build otherwise.
- No `java.sql` anywhere. DB access via Hibernate Reactive or the reactive PG client (cursor streaming).
- Streams to clients: SSE with `@RestStreamElementType(MediaType.APPLICATION_JSON)` returning `Multi`.
  Every client-facing stream must be bounded per client (throttle/sample or `onOverflow().drop()`)
  and must not hold resources after cancellation.
- External connections (WebSocket) must reconnect with backoff (`onFailure().retry().withBackOff(...)`).

## Kafka rules (ADR-0004)
- Topics, keys and payloads are defined in `docs/architecture.md#kafka-topics` — update it when adding one.
- Key = business key (symbol, portfolioId). Payloads are versioned JSON records in `adapter..messaging`.
- Consumers must be idempotent; poison messages go to DLQ (`failure-strategy=dead-letter-queue`).
- Live SSE fan-out consumers use a per-instance group id + `auto.offset.reset=latest`.

## Code conventions
- Records for DTOs, events, value objects. No Lombok. Constructor injection (no `@Inject` fields).
- Money/prices/quantities: `BigDecimal` (never `double`). Time: `Instant` (UTC). Symbols uppercase.
- Config via `@ConfigMapping` interfaces, prefix `market-pulse`. No hard-coded URLs/topics in code.
- Logging: `io.quarkus.logging.Log` or JBoss Logger. No `System.out`, no JUL.
- Errors: map to RFC 7807 problem responses in `adapter.in.rest`; never leak stack traces.

## Database
- Schema only via Flyway: new file `src/main/resources/db/migration/V<n>__<description>.sql`.
- Never modify a committed migration (a hook blocks it). Fix forward with a new migration.

## Testing
- Domain: plain JUnit 5 + AssertJ, no Quarkus.
- Adapters/use cases: `@QuarkusTest` (Dev Services provide Kafka/Postgres); Hibernate Reactive tests with
  `@RunOnVertxContext` + `UniAsserter`; messaging with the in-memory connector where Kafka isn't the point.
- SSE: consume with REST client / `Multi` in test, assert with `AssertSubscriber` or Awaitility.
- Coverage gate: 70 % lines (JaCoCo, `./mvnw verify`). Don't game it with trivial tests.

## Workflow
- One backlog item per branch (`feature/<id>-<slug>`), small commits, PR to `main`. Never push to `main`.
- Plan first for anything touching more than one layer; follow the backlog acceptance criteria.
- New dependency, new topic, or deviation from an ADR → stop and ask. Architectural change → new ADR.
- Done = `./mvnw verify` green + docs updated (architecture.md / domain.md / backlog status).
