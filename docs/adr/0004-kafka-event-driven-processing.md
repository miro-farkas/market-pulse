# ADR-0004: Kafka with SmallRye Reactive Messaging; in-memory state per partition

- Status: Accepted
- Date: 2026-10-02

## Context
We need durable event flow between ingestion, processing and client streams, and the processing must be
explainable in a demo. Candle aggregation and alert evaluation are stateful per symbol.

## Decision
- SmallRye Reactive Messaging (`quarkus-messaging-kafka`) with JSON payloads (records), versioned.
- Topics and keys as in `architecture.md#5-kafka-topics`; key by symbol / portfolioId so all events for an
  aggregate land in one partition and are processed in order by one instance.
- Stateful processing keeps state in memory per owned partition. On rebalance, open candle windows are lost.
- Processing consumers use shared consumer groups; live SSE fan-out consumers use per-instance group ids
  with `auto.offset.reset=latest`.
- Delivery: at-least-once. Events carry `eventId`; consumers are idempotent (DB upserts, cooldowns).
- Poison messages: `failure-strategy=dead-letter-queue` to `<topic>.dlq`.

## Consequences
+ Simple, readable processors; easy to test with the in-memory connector.
− State loss on rebalance/restart (acceptable for a demo; documented).
− Ordering only guaranteed per key.

## Alternatives considered
- Kafka Streams (`quarkus-kafka-streams`): fault-tolerant state stores and windowing, but a different
  programming model that would hide the Mutiny story. Candidate for a later ADR.
