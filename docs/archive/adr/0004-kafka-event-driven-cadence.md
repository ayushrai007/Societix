# ADR-0004: Event-driven core via Postgres outbox now; message broker deferred

**Status:** Accepted (supersedes the earlier "Kafka backbone" draft)
**Date:** 2026-10-29
**Deciders:** Principal Engineer, Backend Lead

## Context

The BRD describes an **"autonomous ERP engine"**: payments auto-match to invoices, the defaulter
cadence escalates on its own (Day +1 → +30), and notifications fan out — all as **reactions to
events**, not synchronous request/response. Forces:

- **Decoupling:** the treasury module shouldn't call the notification or governance modules
  directly; it should emit facts ("invoice settled", "defaulter stage advanced") that others react
  to. This keeps the Modulith boundaries clean
  ([ADR-0001](0001-modular-monolith-spring-modulith.md)) and preserves the extraction seam for later.
- **Consistency:** a payment must post to the ledger **and** emit `payment.settled` atomically — no
  "ledger updated but event lost" or "event sent but ledger rolled back".
- **Auditability:** events feed the append-only, hash-chained audit log
  ([`../03-data-model.md`](../03-data-model.md) §5).
- **Cost & team size:** the first version serves a **low-volume pilot** (BRD Phase 1: ~20 societies,
  ~24k units Yr1) run by a **3-developer team**. A Kafka cluster (brokers, topics, retention,
  consumer-group monitoring, a second region for mirroring) is **operationally and financially
  overkill** at this load — it would cost real money and ops attention we don't have, to buffer a
  throughput we don't yet have.

## Decision

**v1 uses a Postgres transactional outbox plus in-process Spring events and scheduled pollers as the
event backbone. There is no message broker. Kafka (or Redis Streams) is a documented, deferred
upgrade, gated on scale.**

- Domain events are written to an **`outbox` table in the same DB transaction** as the state change
  (e.g. ledger posting), so the DB write and the event are **atomic** — the event is recorded **iff**
  the transaction commits. No dual-write.
- **Dispatch:** a `@Scheduled` poller — guarded by **ShedLock** so exactly one app instance fires —
  reads unprocessed outbox rows and invokes the relevant handlers. Purely local, same-transaction
  reactions may also use Spring `@TransactionalEventListener` / Modulith
  `@ApplicationModuleListener`.
- **Time-based autonomy** (defaulter cadence Day +1/+10/+15/+30, 30-day visitor-data purge) is driven
  by scheduled jobs that query *due* rows — no broker needed for "do X after N days".
- Events carry **`tenantId`**; the event **contract** (names, payloads in
  [`../01-architecture.md`](../01-architecture.md) §5) is **transport-agnostic**.
- Delivery is **at-least-once**; consumers are **idempotent** (Redis idempotency keys + DB unique
  constraints), the same discipline as the PA webhook dedupe
  ([ADR-0003](0003-payment-aggregator-no-float.md)). *We pay the idempotency cost now so the broker
  upgrade is purely a transport swap later.*

### Upgrade trigger (when to introduce Kafka)

Revisit when any of: sustained outbox dispatch lag under normal load; cross-service consumers needed
after a module is extracted; a need for durable, replayable, partitioned log semantics or long audit
replay; or per-tenant event volume that the single-poller model can't keep warm. At that point, add a
relay that drains the **same** outbox rows to Kafka — handlers and event contracts are unchanged.

## Consequences

**Pros**
- **No broker to run or pay for** — reuses PostgreSQL, which we already operate. Smallest possible
  infra footprint for a 3-dev team and a low-volume pilot.
- Outbox still guarantees ledger/event **atomicity** without distributed transactions.
- The event contract is identical to the eventual Kafka design, so the upgrade is a **drop-in
  transport swap**, not a redesign.
- Fewer moving parts → faster local setup (no Kafka in docker-compose) and simpler CI.

**Cons / costs**
- The single scheduled poller is a **throughput ceiling** (fine at pilot scale; the explicit trigger
  above tells us when it isn't).
- Polling adds a little **latency** vs a push broker, and some steady DB poll load (bounded by
  indexing the outbox on `(processed, created_at)` and a sane interval).
- At-least-once still forces **idempotency everywhere** — carried now deliberately, so nothing has to
  change in consumers when a broker arrives.
- No built-in replayable log yet; audit replay relies on the audit table and retained outbox rows.

## Alternatives considered

- **Apache Kafka now (the earlier draft decision).** Best-in-class durable, replayable, partitioned
  log — but brokers, retention tuning, consumer-group monitoring, and (for HA) a second region are
  **cost and ops we can't justify at pilot load** with 3 developers. **Deferred**, not rejected: it is
  the sanctioned upgrade once the trigger conditions hit.
- **Redis Streams / Pub-Sub.** Lighter than Kafka and Redis is already in the stack; a reasonable
  *middle* upgrade. But the Postgres outbox gives stronger write-atomicity (same transaction as the
  business change) with **zero** additional infra, so it wins for v1. Redis Streams remains a valid
  intermediate step before Kafka.
- **DB polling with no outbox (cron reads business tables directly).** Simplest, but scatters "what
  triggers what" across cron logic, risks missing state transitions, and gives no clean event record
  for audit. The outbox is barely more code and far more robust. Rejected.
- **Dual-write (write DB, then publish without outbox).** Loses atomicity — a crash between the two
  writes drops or duplicates events. Rejected; the outbox exists precisely to avoid this.

---

**Related:** [`../01-architecture.md`](../01-architecture.md) §5 ·
[`../03-data-model.md`](../03-data-model.md) §2, §5 ·
[ADR-0001](0001-modular-monolith-spring-modulith.md) ·
[ADR-0003](0003-payment-aggregator-no-float.md) ·
[`../glossary.md`](../glossary.md)
