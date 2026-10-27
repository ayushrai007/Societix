# ADR-0004: Kafka event backbone with transactional outbox

**Status:** Accepted
**Date:** 2026-10-27
**Deciders:** Principal Engineer, Backend Lead

## Context

The BRD describes an **"autonomous ERP engine"**: payments auto-match to invoices, the defaulter
cadence escalates on its own (Day +1 → +30), and notifications fan out — all as **reactions to
events**, not synchronous request/response. Forces:

- **Decoupling:** the treasury module shouldn't call the notification or governance modules
  directly; it should emit facts ("invoice settled", "defaulter stage advanced") that others react
  to. This also keeps the Modulith boundaries clean ([ADR-0001](0001-modular-monolith-spring-modulith.md))
  and preserves the extraction seam for Phase 3.
- **Consistency:** a payment must post to the ledger **and** emit `payment.settled` atomically — no
  "ledger updated but event lost" or "event sent but ledger rolled back".
- **Auditability:** events feed the append-only, hash-chained audit log
  ([`../03-data-model.md`](../03-data-model.md) §5).
- The architecture already defines Kafka topics keyed by `tenantId` and commits to an outbox +
  at-least-once + idempotent consumers ([`../01-architecture.md`](../01-architecture.md) §5).

## Decision

**We will use Apache Kafka as the event backbone, with the transactional outbox pattern for
reliable publication, and at-least-once delivery consumed by idempotent handlers.**

- Domain events are written to an **outbox table in the same DB transaction** as the state change
  (e.g. ledger posting). A relay drains the outbox to Kafka, so the DB write and the event are
  **atomic** — the event is published **iff** the transaction commits.
- Topics are keyed by **`tenantId`** for per-tenant ordering and partition locality
  (`billing.invoice.raised`, `payment.settled`, `defaulter.stage.advanced`, `agm.vote.cast`,
  `gate.visit.logged`, `privacy.purge.due`, `audit.action.recorded` — see
  [`../01-architecture.md`](../01-architecture.md) §5).
- Delivery is **at-least-once**; consumers are **idempotent** (Redis idempotency keys + DB unique
  constraints) so redelivery never double-posts (same discipline as the PA webhook dedupe in
  [ADR-0003](0003-payment-aggregator-no-float.md)).
- Within the modular monolith, local reactions may use Modulith `@ApplicationModuleListener`; any
  event that must survive process boundaries or drive the autonomous cadence goes through Kafka via
  the outbox.

## Consequences

**Pros**
- Reliable, decoupled, auditable event flow; the "autonomous" cadence is just consumers reacting to
  topics.
- Outbox guarantees ledger/event consistency without distributed transactions.
- `tenantId` partition keys give ordering per society and a clean path to scale/extract modules.
- Replayable log: consumers can be rebuilt or back-filled from Kafka; great for audit and recovery.

**Cons / costs**
- Kafka is operationally non-trivial (brokers, topics, retention, consumer-group monitoring);
  justified by the event-driven core, accepted as core infra.
- At-least-once forces **idempotency everywhere** — a real design burden on every consumer.
- The outbox adds a table and a relay to operate and monitor (lag alerts).
- Eventual consistency: downstream state (notifications, invoice `PAID`) lags the triggering action
  slightly — acceptable and expected for this domain.

## Alternatives considered

- **DB polling / scheduled jobs** for the cadence. Simple, but adds latency, hammers the DB, and
  scatters "what triggers what" across cron logic with no replay or audit stream. Rejected as the
  primary mechanism (a scheduler still *emits* events, it doesn't replace the backbone).
- **RabbitMQ (or similar broker).** Great for task queues, but weaker at a durable, replayable,
  partitioned event **log** and long retention/audit replay. The BRD's auditable event stream fits
  Kafka's log model better. Rejected.
- **In-process events only (no Kafka).** Zero infra and fine *inside* the monolith, but gives no
  durable/replayable stream, no cross-process delivery, and no extraction seam for Phase 3. We keep
  in-process listeners for purely local reactions but not for the autonomous backbone. Rejected as
  the sole mechanism.
- **Dual-write (write DB, then publish without outbox).** Simplest to code but loses atomicity —
  a crash between the two writes drops or duplicates events. Rejected; the outbox exists precisely to
  avoid this.

---

**Related:** [`../01-architecture.md`](../01-architecture.md) §5 ·
[`../03-data-model.md`](../03-data-model.md) §2, §5 ·
[ADR-0001](0001-modular-monolith-spring-modulith.md) ·
[ADR-0003](0003-payment-aggregator-no-float.md) ·
[`../glossary.md`](../glossary.md)
