# market-pulse

Reactive, event-driven demo backend: live crypto market data → Kafka → stream processing → reactive SSE APIs.
Java 25 · Quarkus · Mutiny · Kafka (SmallRye Reactive Messaging) · Hibernate Reactive · PostgreSQL.

- Architecture: [docs/architecture.md](docs/architecture.md)
- Domain: [docs/domain.md](docs/domain.md)
- Decisions: [docs/adr/](docs/adr)
- Backlog: [docs/backlog.md](docs/backlog.md)

## Prerequisites
JDK 25, Maven 3.9+, Docker (Dev Services start Kafka and PostgreSQL), `gh` CLI, Claude Code.

## First-time setup
```bash
./bootstrap.sh market-pulse          # wrapper, format, verify, GitHub repo, branch protection
```

## Run
```bash
./mvnw quarkus:dev                              # live Binance data
./mvnw quarkus:dev -Dquarkus.profile=simulator  # offline, synthetic ticks
```
Dev UI: http://localhost:8080/q/dev-ui · Swagger UI: http://localhost:8080/q/swagger-ui

## Guardrails
| Layer | What | Where |
|---|---|---|
| Context | Rules, architecture, conventions for Claude | `CLAUDE.md`, `docs/` |
| Build | Layering, no blocking outside VT/@Blocking, no JDBC, entities placement, conventions | `ArchitectureTest` (ArchUnit) |
| Build | Formatting | Spotless (palantir-java-format) |
| Build | Java/Maven versions, banned dependencies (classic RESTEasy, ORM Panache, Lombok, Spring) | Maven Enforcer |
| Build | ≥ 70 % line coverage | JaCoCo |
| Agent | Permission allow/ask/deny; guardrail files are read-only for Claude | `.claude/settings.json` |
| Agent | Format on edit; block edits of committed migrations; ArchUnit check before finishing | `.claude/hooks/` |
| Repo | PR required, CI green, no force push to `main` | branch protection + `.github/workflows/ci.yml` |

## Working with Claude Code
```text
/implement M1      # plan → approve → implement on a branch → verify → commit
/review            # strict review of the branch against ADRs
/adr <title>       # draft a new decision record
```
Then push and open a PR; CI must be green before merge.
