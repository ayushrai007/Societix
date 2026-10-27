# 01 — Architecture

**Document:** `SOC-DOC-01` · **Status:** Anchor document — all other docs reference this one.

This document defines the system architecture: the C4 views, the event-driven topology, the
multi-tenancy model, and how the BRD non-functional requirements map to design decisions.

---

## 1. Architectural principles

1. **Modular monolith first.** One deployable Spring Boot application partitioned into
   well-bounded modules via **Spring Modulith**; extract to services only when scale demands it.
   Rationale in [ADR-0001](adr/0001-modular-monolith-spring-modulith.md).
2. **Event-driven core.** Treasury allocation, defaulter cadence, and notifications are driven by
   domain events on **Kafka** — the "autonomous" behavior is asynchronous reactions to events.
3. **Privacy & tenancy by construction.** Every row is tenant-scoped; PostgreSQL **RLS** enforces
   isolation at the database, not just the application. No third-party tracking SDKs anywhere.
4. **No float.** Money never flows through Societix; the Payment Aggregator settles to the society
   account and notifies us via webhook.
5. **Offline-tolerant edge.** The gate app is authoritative offline (local SQLite) and reconciles
   via CRDTs on reconnect.
6. **Auditable & deterministic.** Ledger postings and votes are append-only and tamper-evident.

## 2. C4 — Level 1: System Context

```mermaid
graph TB
    subgraph Users
      MC[MC Officer / Committee]
      RES[Resident / Tenant]
      GUARD[Security Guard]
      AUD[Statutory Auditor]
    end
    SOC[("Societix<br/>Community OS")]
    PA[Payment Aggregator<br/>Razorpay/Cashfree]
    BANK[Society Bank Account]
    WA[WhatsApp / SMS / IVR]
    LLM[LLM Provider]
    ANPR[ANPR Camera / Gate HW]

    MC --> SOC
    RES --> SOC
    GUARD --> SOC
    AUD --> SOC
    SOC -->|VAN/VPA, payouts| PA
    PA -->|settles| BANK
    PA -->|webhooks| SOC
    SOC -->|reminders, notices| WA
    SOC -->|RAG queries| LLM
    ANPR --> SOC
```

## 3. C4 — Level 2: Containers

```mermaid
graph TB
    subgraph Client
      WEB[Web Console<br/>React+TS]
      MOB[Resident App<br/>React Native]
      GATE[Gate App<br/>RN + SQLite/CRDT]
    end
    subgraph Edge
      GW[API Gateway / BFF<br/>Spring Cloud Gateway]
    end
    subgraph Core["Societix Modular Monolith (Spring Boot 3 + Modulith)"]
      M1[M1 Treasury/ERP]
      M2[M2 Governance/AGM]
      M3[M3 AI Copilot]
      M4[M4 Gate Sync]
      M5[M5 Privacy Shield]
      M6[M6 Smart-Switch ETL]
      IAM[Identity & RBAC]
      NOTIF[Notification Service]
      AUDIT[Audit Log]
    end
    subgraph Data
      PG[(PostgreSQL 16<br/>RLS + pgvector)]
      REDIS[(Redis)]
      KAFKA[(Kafka)]
      OBJ[(Object Storage<br/>docs, audit packs)]
    end
    WEB --> GW
    MOB --> GW
    GATE --> GW
    GW --> IAM
    GW --> M1 & M2 & M3 & M4 & M5 & M6
    M1 & M2 & M4 & M5 --> KAFKA
    NOTIF --> KAFKA
    M1 & M2 & M3 & M4 & M5 & M6 --> PG
    M3 --> PG
    IAM --> REDIS
    M1 --> REDIS
    AUDIT --> PG
```

## 4. C4 — Level 3: Component (illustrative — M1 Treasury)

```mermaid
graph LR
    subgraph M1 [Treasury/ERP Module]
      BILL[Billing Engine<br/>multi-formula]
      VAN[VAN/VPA Provisioner]
      MATCH[Webhook Matcher]
      LEDGER[Double-Entry Ledger]
      CADENCE[Defaulter Cadence Orchestrator]
      PAYOUT[Maker-Checker Payouts]
    end
    PA[(Payment Aggregator)] -->|payment.credited webhook| MATCH
    MATCH -->|post| LEDGER
    BILL -->|invoice.raised event| KAFKA[(Kafka)]
    MATCH -->|invoice.settled event| KAFKA
    KAFKA --> CADENCE
    CADENCE -->|reminder.due event| KAFKA
    VAN --> PA
    PAYOUT --> PA
```

## 5. Event-driven topology

The BRD's "autonomous ERP engine" is realized as event producers/consumers on Kafka. Core topics
(all keyed by `tenantId` for ordering & partition locality):

| Topic | Produced by | Consumed by | Purpose |
|---|---|---|---|
| `billing.invoice.raised` | M1 Billing | M1 Ledger, Notification | New invoice generated |
| `payment.settled` | M1 Matcher (from PA webhook) | M1 Ledger, M1 Cadence | Zero-touch allocation |
| `payment.unmatched` | M1 Matcher | Ops queue | Needs manual allocation |
| `defaulter.stage.advanced` | M1 Cadence | Notification | Day +1/+10/+15/+30 escalation |
| `agm.vote.cast` | M2 Voting | M2 Tally, Audit | Cryptographic ballot event |
| `gate.visit.logged` | M4 Sync | M5 Purge scheduler, Notification | Visitor event (30-day TTL) |
| `privacy.purge.due` | M5 Scheduler | M5 Purge worker | 30-day auto-delete of visitor PII |
| `audit.action.recorded` | all | Audit log (append-only) | Tamper-evident trail |

**Delivery semantics:** at-least-once; consumers are idempotent (Redis idempotency keys +
unique constraints). Ledger postings use an **outbox pattern** (transactional outbox → Kafka) to
guarantee ledger-event consistency.

## 6. Multi-tenancy model

- **Isolation:** shared schema, **row-level** tenancy. Every tenant-owned table has a non-null
  `tenant_id`; PostgreSQL **RLS** policies filter by a session GUC (`app.tenant_id`) set per request.
- **Context propagation:** the API gateway resolves `tenantId` from the authenticated principal and
  passes it downstream; a Spring `OncePerRequestFilter` opens a connection and executes
  `SET LOCAL app.tenant_id = :tid` inside the request transaction.
- **Keys:** per-tenant data-encryption keys (envelope encryption) for sensitive columns (visitor PII,
  bank identifiers). See [ADR-0002](adr/0002-postgres-rls-multitenancy.md) and
  [`03-data-model.md`](03-data-model.md).
- **Noisy-neighbor:** per-tenant rate limits (Redis) at the gateway; large tenants can be pinned to
  dedicated Kafka partitions.

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as API Gateway
    participant APP as Spring App (RLS filter)
    participant DB as PostgreSQL (RLS)
    C->>GW: request + JWT (tenant, roles)
    GW->>APP: forward + X-Tenant-Id
    APP->>DB: BEGIN; SET LOCAL app.tenant_id = tid
    APP->>DB: query (RLS auto-filters by tenant_id)
    DB-->>APP: tenant-scoped rows only
    APP-->>C: response
```

## 7. NFR → design mapping (BRD §10)

| NFR | Design response |
|---|---|
| Gate check-in < 1.5 s | Edge-local decision (SQLite); async push via FCM/APNs high-priority; gate reads never hit cloud on the hot path |
| Zero loss over 6-hr blackout | SQLite WAL + CRDT merge on reconnect; see [dd-02](deep-dives/dd-02-offline-gate-crdt.md) |
| 99.95% availability | Stateless app pods (HPA), multi-region active-passive, PG failover, Kafka replication |
| AI query < 2.5 s | Pre-computed pgvector embeddings of bylaws & ledger; retrieval-augmented prompt; result cache in Redis |
| Data isolation | PostgreSQL RLS + per-tenant keys; tested by tenancy integration tests |
| Immutable audit | Append-only, hash-chained audit table; writes via DB trigger, no UPDATE/DELETE grant |

## 8. Cross-cutting concerns

Detailed in [`02-system-design.md`](02-system-design.md): authN/authZ (OIDC + RBAC), the uniform
error model, idempotency, pagination, i18n (8 languages), the notification fan-out (in-app →
WhatsApp → SMS → IVR), and observability.

## 9. Deployment topology

```mermaid
graph TB
    subgraph "Region: Mumbai (active)"
      LB1[Ingress] --> P1[App pods HPA]
      P1 --> PGm[(PG primary)]
      P1 --> Rm[(Redis)]
      P1 --> Km[(Kafka)]
    end
    subgraph "Region: Hyderabad (passive)"
      PGh[(PG standby)]
      Kh[(Kafka mirror)]
    end
    PGm -->|streaming replication| PGh
    Km -->|mirror| Kh
```

Infra details (K8s manifests, IaC, secrets, rollback) in [`delivery/cicd-devops.md`](delivery/cicd-devops.md).

## 10. Key architecture decisions (ADRs)

| ADR | Decision |
|---|---|
| [0001](adr/0001-modular-monolith-spring-modulith.md) | Modular monolith with Spring Modulith |
| [0002](adr/0002-postgres-rls-multitenancy.md) | PostgreSQL RLS for multi-tenancy |
| [0003](adr/0003-payment-aggregator-no-float.md) | Payment Aggregator, no float held |
| [0004](adr/0004-kafka-event-driven-cadence.md) | Kafka event-driven cadence + outbox |
| [0005](adr/0005-offline-gate-sqlite-crdt.md) | Offline gate: SQLite + CRDT |
| [0006](adr/0006-crypto-evoting-scheme.md) | Cryptographic e-voting scheme |
