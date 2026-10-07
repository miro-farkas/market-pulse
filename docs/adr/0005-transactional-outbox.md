# ADR-0005: Transactional outbox for reference-data change events

- Status: Accepted (phase 1), Proposed (phase 2)
- Date: 2026-10-02

## Context
Processors cache portfolios, holdings and alert rules and must learn about changes. Writing to the DB and
publishing to Kafka separately risks dual-write inconsistency.

## Decision
- Phase 1: each CRUD transaction also inserts an `outbox` row. A reactive relay polls unpublished rows
  (`SELECT ... FOR UPDATE SKIP LOCKED`, small batches), publishes to `reference-data.changes`, and marks them
  published. Consumers are idempotent by `eventId`.
- Phase 2 (optional): replace the relay with Debezium CDC (outbox event router). Topic and payload stay the
  same, so consumers don't change.
- On startup, processors load the full reference data as a `Multi` from the DB, then apply change events.

## Consequences
+ No dual writes; demonstrates a core event-driven pattern.
− Polling adds latency (configurable, default 500 ms) and DB load.
− Phase 2 needs Kafka Connect/Debezium infrastructure not provided by Dev Services (docker compose).

## Alternatives considered
- Publish directly after commit: lost events on crash.
- Debezium from day one: more infrastructure before any feature works.
