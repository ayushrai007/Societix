# ADR-0008: PostgreSQL transactional outbox, no message broker in v1

**Status:** Accepted (supersedes archived [ADR-0004](../archive/adr/0004-kafka-event-driven-cadence.md))  
**Date:** 2026-11-08  
**Deciders:** Principal Engineer, Backend Lead, CTO

## Context

Societix is built around an **autonomous ERP engine**: maintenance payments auto-match to invoices,
the defaulter cadence escalates autonomously (Day +1 → +30), and notifications fan out to residents
and committee members — all driven by domain events rather than tight synchronous RPC coupling.

Forces:
- **Decoupling:** Modules (Treasury, Notifications, Governance, Gate, Privacy) must react to domain
  facts ("invoice settled", "defaulter stage advanced") without violating Spring Modulith module
  boundaries or creating cyclic compile-time dependencies.
- **Consistency:** A ledger posting and its corresponding event (e.g., `payment.settled`) must
  commit atomically in the database — no "ledger updated but event lost" or "event emitted but DB
  transaction rolled back" dual-write inconsistencies.
- **Auditability:** Domain events feed the append-only, tamper-evident audit log.
- **Team capacity & pilot scale:** The v1 pilot serves ~20 societies (~24k units in Year 1) built
  and operated by a **3-developer team**. Operating a Kafka or RabbitMQ broker cluster (broker
  provisioning, topics, partition balancing, consumer-group monitoring, cross-region mirroring) is
  operationally and financially unjustifiable overhead at this stage.

## Decision

**v1 uses a PostgreSQL transactional outbox table (`outbox`) combined with in-process Spring
events and ShedLock-guarded scheduled pollers as the event backbone. There is no external message
broker in v1.**

1. **Transactional Outbox Table:** Domain events are written to an `outbox` table within the same
   database transaction as the business entity change. If the transaction commits, the event is
   guaranteed to be recorded; if it rolls back, the event is discarded. No dual-write is permitted.
2. **Dispatch Mechanism:** A `@Scheduled` poller — guarded by **ShedLock** so exactly one application
   instance processes a batch at any time — queries unprocessed rows (`processed = false ORDER BY created_at ASC`),
   dispatches them to registered internal listeners, and marks them processed.
3. **In-Process Reactions:** Same-transaction, in-process reactions within the monolith use Spring's
   `@TransactionalEventListener` or Spring Modulith's `@ApplicationModuleListener`.
4. **Time-Based Autonomy:** Scheduled time-based workflows (e.g., daily defaulter progression,
   30-day visitor log purge) query due entity rows directly or via outbox schedules; no complex
   broker delay queues are used.
5. **Transport-Agnostic Contracts:** Events carry `tenantId`, timestamp, event type, and payload in
   JSON format. The schema is transport-agnostic.
6. **At-Least-Once Delivery & Idempotency:** Outbox dispatch guarantees at-least-once delivery. All
   event consumers must be strictly idempotent, backed by database unique constraints and Redis
   idempotency keys.

### Upgrade Trigger (When to introduce a message broker)

We will introduce an external message broker (such as Kafka or Redis Streams) only when one of the
following operational triggers occurs:
- Sustained outbox poller lag under normal traffic exceeds acceptable SLA thresholds.
- A functional module is extracted from the modular monolith into an independent microservice
  requiring asynchronous network transport.
- A hard requirement arises for long-term partitioned event streaming or high-throughput external
  event replay.

When triggered, an outbox relay worker will drain rows from the existing `outbox` table to the
message broker without modifying the domain logic or event payload contracts.

## Consequences

**Pros**

- **Zero broker operational overhead:** Reuses PostgreSQL, which the team already operates and
  backs up. No ZooKeeper/KRaft, broker clusters, or broker monitoring in local development or CI.
- **Guaranteed atomicity:** Business state changes and event publication share one local database
  transaction. Dual-write failure modes are eliminated.
- **Simple local dev environment:** Docker Compose requires only PostgreSQL (with pgvector) and
  Redis; setup is fast and lightweight.
- **Clean migration path:** Because contracts and idempotency are established up front, swapping
  the poller dispatch for a Kafka publisher is a drop-in transport change.

**Cons / costs**

- Polling introduces slight delivery latency compared to immediate broker pub/sub.
- The single-poller model has an upper throughput ceiling (well above v1 pilot requirements).
- Developers must maintain strict consumer idempotency discipline from day one.

## Alternatives considered

- **Apache Kafka in v1:** Best-in-class distributed log, but requires substantial operational
  attention and hosting costs unjustified by 20 pilot societies. Deferred to Phase 3 scale.
- **Redis Streams / Pub-Sub:** Lighter than Kafka, but Redis pub/sub does not share a transaction
  with PostgreSQL, re-introducing dual-write consistency failure modes unless paired with an
  outbox anyway.
- **Direct Database Polling without Outbox:** Having cron jobs poll state flags on individual domain
  tables scatters event logic across services and lacks an immutable audit trail of domain events.
- **Dual-write without Outbox:** Updating PostgreSQL then publishing to a message channel leads to
  ghost events or missing events during application crashes or network partitions. Strictly rejected.

---

**Related:** [ADR-0001](../archive/adr/0001-modular-monolith-spring-modulith.md) ·
[ADR-0007](0007-maven-supported-version-baseline.md) ·
[`../archive/adr/0004-kafka-event-driven-cadence.md`](../archive/adr/0004-kafka-event-driven-cadence.md) ·
[`05-building-it.md`](../05-building-it.md)
