# ADR-0007: Java 25 as the language baseline

- Status: Accepted
- Date: 2026-10-07

## Context
The project started on Java 21. Java 25 is the current LTS release and Quarkus 3.40 supports it.
Since Java 24 (JEP 491), virtual threads no longer pin their carrier inside `synchronized` blocks,
which removes the pinning risk noted in ADR-0002 for the virtual-thread boundary.

## Decision
Java 25 is the minimum and target version: `maven.compiler.release=25`, Maven Enforcer
`requireJavaVersion [25,)`, and CI builds with Temurin 25.

## Consequences
- Contributors and CI need JDK 25. The README, CLAUDE.md and bootstrap script state this.
- Language and library features up to Java 25 may be used. The reactive rules of ADR-0002 are
  unchanged: Mutiny stays the default and blocking code is still confined to `@RunOnVirtualThread`/`@Blocking`.
- ADR-0002's note on pinning under Java 21 no longer applies to `synchronized`; native frames can still pin.

## Alternatives considered
- Stay on Java 21 (LTS) – works, but keeps the `synchronized` pinning caveat and is older than needed for a new demo.
