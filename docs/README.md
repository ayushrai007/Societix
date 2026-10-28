# Societix — Engineering Documentation

> **Societix** is an autonomous, privacy-first *Residential Community Operating System* for
> Indian gated communities. It replaces the manual, spreadsheet-driven work of volunteer Managing
> Committees with zero-touch treasury, statutory e-AGM governance, an AI copilot, and offline-first
> gate security — with a **zero-ad, DPDP-compliant** guarantee.
>
> Source of truth for product scope: **BRD `SOC-BRD-2026-V1`**.

This folder is the developer documentation set. A new engineer should be able to read the core
docs, follow `standards/getting-started.md`, pick up a module, and start coding.

## Reading order

1. **[00 — Product Overview](00-product-overview.md)** — vision, personas, scope, module map.
2. **[01 — Architecture](01-architecture.md)** — C4 views, event topology, multi-tenancy, NFRs. **(anchor doc — read first)**
3. **[02 — System Design](02-system-design.md)** — cross-cutting concerns (auth/RBAC, errors, idempotency, notifications, i18n).
4. **[03 — Data Model](03-data-model.md)** — ERD, core entities, double-entry schema, RLS.
5. **[04 — API Specification](04-api-specification.md)** — REST conventions + endpoint catalogue, webhooks.
6. **[05 — Security & Compliance](05-security-compliance.md)** — DPDP, RBI PA, State Co-op Acts, crypto, threat model.
7. **[06 — Frontend Architecture](06-frontend-architecture.md)** — web, mobile, gate app; design system.

## Module specifications

See **[modules/README.md](modules/README.md)** for the dependency graph and build order.

| # | Module | BRD FRs | Doc |
|---|--------|---------|-----|
| 1 | Autopilot Treasury, Billing & Double-Entry ERP | FR-1.1 … FR-1.5 | [module-1-treasury-erp.md](modules/module-1-treasury-erp.md) |
| 2 | Statutory Governance & e-AGMs | FR-2.1 … FR-2.4 | [module-2-governance-agm.md](modules/module-2-governance-agm.md) |
| 3 | AI Committee Copilot | FR-3.1 … FR-3.4 | [module-3-ai-copilot.md](modules/module-3-ai-copilot.md) |
| 4 | Offline Gate Infrastructure | FR-4.1 … FR-4.3 | [module-4-gate-infra.md](modules/module-4-gate-infra.md) |
| 5 | DPDP Act Privacy Shield | FR-5.1 … FR-5.3 | [module-5-privacy-shield.md](modules/module-5-privacy-shield.md) |
| 6 | Smart-Switch Migration | FR-6.1 … FR-6.3 | [module-6-smart-switch.md](modules/module-6-smart-switch.md) |

## Deep dives (hardest subsystems)

- **[dd-01 — Double-Entry Ledger](deep-dives/dd-01-double-entry-ledger.md)**
- **[dd-02 — Offline Gate CRDT Sync](deep-dives/dd-02-offline-gate-crdt.md)**
- **[dd-03 — Cryptographic e-Voting](deep-dives/dd-03-crypto-evoting.md)**
- **[dd-04 — AI Copilot / RAG](deep-dives/dd-04-ai-copilot-rag.md)**

## Delivery & project management

- **[development-plan.md](delivery/development-plan.md)** — phased plan (BRD Phases 0–3) → sprints.
- **[epics-and-stories.md](delivery/epics-and-stories.md)** — epics → user stories with acceptance criteria.
- **[estimation-and-team.md](delivery/estimation-and-team.md)** — team topology, roles, sizing, RACI.
- **[testing-strategy.md](delivery/testing-strategy.md)** — test pyramid, contract/e2e/perf/compliance tests.
- **[cicd-devops.md](delivery/cicd-devops.md)** — pipelines, environments, IaC, release/rollback.
- **[definition-of-done.md](delivery/definition-of-done.md)** — DoR/DoD, quality gates, PR checklist.

## Standards

- **[getting-started.md](standards/getting-started.md)** — local setup (start here to run the stack).
- **[coding-standards.md](standards/coding-standards.md)** — Java/Spring conventions.
- **[api-guidelines.md](standards/api-guidelines.md)** — REST design rules.
- **[git-workflow.md](standards/git-workflow.md)** — branching, commits, reviews.

## Architecture Decision Records

See **[adr/README.md](adr/README.md)** for the index. Key decisions: modular monolith (Spring
Modulith), PostgreSQL RLS multi-tenancy, payment-aggregator (no float), Kafka event cadence,
offline gate CRDT, cryptographic e-voting scheme.

## Glossary

Domain and regulatory terms are defined in **[glossary.md](glossary.md)**.

---

## Canonical technology stack

This stack is **authoritative** — all docs and code must align to it. Changes go through an ADR.

| Layer | Choice |
|-------|--------|
| Language / runtime | **Java 21** (LTS) |
| Framework | **Spring Boot 3.3+**, **Spring Modulith** (modular monolith first) |
| Build | **Gradle 8.x** (Kotlin DSL) |
| Persistence | **PostgreSQL 16** with **Row-Level Security (RLS)** multi-tenancy; **Flyway** migrations |
| Vector search | **pgvector** (bylaws + ledger embeddings for the AI Copilot) |
| Cache / coordination | **Redis 7** (cache, rate-limit, idempotency keys, locks) |
| Messaging | **Apache Kafka** (event-driven topology, webhook matcher, defaulter cadence) |
| Payments | **Razorpay / Cashfree** Payment Aggregator (VAN + UPI VPA); Societix holds **no float** |
| Web frontend | **React 18 + TypeScript + Vite**, TanStack Query, Radix/shadcn, navy+emerald tokens |
| Mobile (resident) & Gate app | **React Native (Expo)**; gate app = local **SQLite + CRDT** offline sync |
| AI | RAG over pgvector; LLM provider abstraction (claude-sonnet-5-5 default) |
| Infra | **Docker + Kubernetes** on sovereign Indian cloud (AWS/GCP Mumbai & Hyderabad) |
| Security | TLS 1.3 + HSTS; AES-256 at rest w/ envelope key rotation; immutable audit trail |
| Observability | **OpenTelemetry**, Prometheus, Grafana; centralized structured logging |

### Module numbering (authoritative, maps 1:1 to BRD) 

`M1` Treasury/ERP · `M2` Governance/e-AGM · `M3` AI Copilot · `M4` Gate Infra · `M5` Privacy Shield · `M6` Smart-Switch.
