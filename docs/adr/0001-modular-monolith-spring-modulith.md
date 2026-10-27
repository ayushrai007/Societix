# ADR-0001: Modular monolith with Spring Modulith

**Status:** Accepted
**Date:** 2026-10-27
**Deciders:** Principal Engineer, Backend Lead, CTO

## Context

Societix is a greenfield product built by a small early team against a broad BRD spanning six
cohesive but distinct domains — **M1 Treasury/ERP, M2 Governance/e-AGM, M3 AI Copilot, M4 Gate
Infra, M5 Privacy Shield, M6 Smart-Switch**. We need:

- **Fast iteration** early — one codebase, one deploy, trivial local setup (see
  [`../standards/getting-started.md`](../standards/getting-started.md)).
- **Clear internal boundaries** so the six domains don't congeal into a big ball of mud, and so we
  can extract services later if scale demands (BRD Phase 3, ~440k units).
- **Strong transactional guarantees** across the double-entry ledger and the outbox — hard to keep
  correct across network boundaries early on.
- A tiny team that cannot operate a fleet of microservices (per-service CI/CD, networking, tracing,
  on-call) while still finding product-market fit.

The architecture anchor already commits to "modular monolith first"
([`../01-architecture.md`](../01-architecture.md) §1).

## Decision

**We will build Societix as a single deployable Spring Boot 3 application, partitioned into
well-bounded modules enforced by Spring Modulith**, one top-level package per module under
`com.societix` (`treasury`, `governance`, `copilot`, `gate`, `privacy`, `migration`, plus a shared
`platform` kernel).

- Modules communicate only via each module's **published events** (preferred) or a narrow
  **`*Facade`** API — never by reaching into another module's internals.
- `ApplicationModules.verify()` runs as a build-blocking test, so boundary violations fail CI.
- In-process domain events use Modulith's `@ApplicationModuleListener`; events that must reach Kafka
  use the transactional **outbox** ([ADR-0004](0004-kafka-event-driven-cadence.md)).
- Boundaries are drawn so that, at scale, a module can be **extracted into its own service** with
  minimal surgery (replace in-process events with Kafka topics it already publishes).

## Consequences

**Pros**
- One deployable, one DB connection pool, one local `docker compose` — fast onboarding and CI.
- Strong ACID transactions across ledger + outbox without distributed-transaction machinery.
- Enforced module boundaries give most of the architectural discipline of microservices without the
  operational tax; refactoring across a boundary is a compile-time concern, not a deploy dance.
- A clear, test-verified seam to extract services later without a rewrite.

**Cons / costs**
- The whole app scales and deploys as a unit — a hot module can't scale independently (mitigated by
  HPA on stateless pods; acceptable until Phase 3).
- Boundary discipline relies on the Modulith verify test and review vigilance; a careless import can
  couple modules if the test is ever skipped.
- A single runtime means a severe fault (e.g. OOM) affects all modules; mitigated by per-module
  resilience and the eventual extraction path.

## Alternatives considered

- **Microservices from day one.** Independent scaling and deploys, but massive operational overhead
  (six+ pipelines, service mesh, distributed tracing, cross-service transactions/saga for the ledger)
  for a pre-PMF team. Rejected as premature; we keep the *option* via clean boundaries.
- **Plain (unmodularized) monolith.** Fastest to start, but with six domains and a multi-year roadmap
  it reliably decays into a ball of mud with no extraction seam. Rejected — Modulith gives the same
  speed with enforced boundaries.
- **Monolith split by layer (controllers/services/repos).** Common but organizes by technical layer,
  not domain; cross-cutting a feature touches every layer package and provides no extraction seam.
  Rejected in favor of package-by-module ([`../standards/coding-standards.md`](../standards/coding-standards.md) §1).

---

**Related:** [`../01-architecture.md`](../01-architecture.md) ·
[`../standards/coding-standards.md`](../standards/coding-standards.md) ·
[ADR-0004](0004-kafka-event-driven-cadence.md)
