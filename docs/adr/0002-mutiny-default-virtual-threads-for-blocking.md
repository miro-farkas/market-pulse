# ADR-0002: Mutiny is the default; virtual threads only at explicit blocking boundaries

- Status: Accepted
- Date: 2026-10-02

## Context
Quarkus offers two scalable models: reactive (Mutiny on the Vert.x event loop) and imperative code on
Java 21 virtual threads. The system is stream-centric: exchange WebSocket → Kafka → processors → SSE.
Virtual threads make blocking request/response cheap but provide no stream abstraction: no backpressure,
operators (merge, sample, window), or cancellation propagation on client disconnect. On Java 21,
`synchronized` blocks still pin carrier threads (fixed only in JDK 24, JEP 491), so blocking libraries can
silently degrade throughput.

## Decision
- All streams use Mutiny `Multi`: ingestion, Kafka processing, SSE endpoints, DB cursor streaming.
- Single results use `Uni`. Persistence is reactive (ADR-0003).
- Blocking is allowed only in methods annotated `@RunOnVirtualThread` (endpoints, consumers) or
  `@Blocking` (marker for service methods called from them). One deliberate showcase:
  `GET /api/reports/portfolios/{id}` using a blocking REST client on a virtual thread.
- Forbidden outside those methods: `Uni.await()`, `Multi.subscribe().asIterable()/asStream()`,
  `Thread.sleep`, `Future.get()`, `CompletableFuture.join()`, `java.sql`.

## Consequences
+ Backpressure and cancellation work end-to-end; slow SSE clients are isolated.
+ Clear, teachable contrast between the two models.
+ Enforced by ArchUnit (`NO_BLOCKING_OUTSIDE_MARKED_METHODS`).
− Mutiny has a learning curve and harder stack traces; mitigated by keeping logic in pure domain code.
− The ArchUnit rule is call-site based: a blocking call inside a lambda in an annotated method may be
  reported; restructure the code rather than suppressing the rule.

## Alternatives considered
- Virtual threads everywhere (Hibernate ORM + JDBC): simpler CRUD, but streaming to clients would need
  manual `SseEventSink` loops without backpressure; DB streaming not reactive; pinning risk on Java 21.
- Mutiny everywhere with no VT: loses the opportunity to show where VT is the better fit.
