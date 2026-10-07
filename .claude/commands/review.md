---
description: Review the current branch against the project's architecture and reactive rules
---
Review `git diff main...HEAD` as a strict senior reviewer. Check, with file:line references:

- Hexagonal boundaries (ADR-0001): framework types in domain/application, entities or DTOs leaking.
- Reactive correctness (ADR-0002): hidden blocking (incl. blocking libraries inside Mutiny operators),
  unbounded buffers, missing per-client bounds on SSE, missing cancellation cleanup, subscriptions created
  per request on hot sources, missing timeouts/retry with backoff.
- Kafka (ADR-0004): keys, idempotency, DLQ, consumer group choice (shared vs per-instance), topic docs.
- Persistence (ADR-0003): transaction boundaries, outbox in the same transaction, migrations fix-forward.
- Domain rules (docs/domain.md): BigDecimal, Instant, crossing semantics, invariants.
- Tests: do they test behaviour (not implementation)? Edge cases? Flaky timing?

Output: blocking issues, should-fix, nits. Don't modify files.
