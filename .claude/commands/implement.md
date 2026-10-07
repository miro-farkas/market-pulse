---
description: Implement a backlog item (e.g. /implement M2) end-to-end on a feature branch
argument-hint: <backlog-id>
---
Implement backlog item **$ARGUMENTS** from `docs/backlog.md`.

1. Read `CLAUDE.md`, the backlog item, and the relevant sections of `docs/architecture.md`, `docs/domain.md`
   and ADRs. If anything is ambiguous or conflicts with an ADR, stop and ask.
2. Create branch `feature/$ARGUMENTS-<short-slug>` from an up-to-date `main`.
3. Present a short plan: classes per layer (domain / application ports+services / adapters), Kafka channels,
   config keys, migrations, tests. Wait for my approval.
4. Implement in small steps: domain + unit tests first, then use cases, then adapters with `@QuarkusTest`s.
5. Run `./mvnw verify` and fix everything until green. Never weaken ArchUnit rules, coverage thresholds or
   enforcer rules; never skip or disable tests.
6. Update `docs/architecture.md` (topics, endpoints), `docs/domain.md` and the backlog status as needed.
7. Commit with conventional messages (`feat(M2): ...`). Summarize: what was built, how to try it
   (curl commands), deviations, follow-ups.
