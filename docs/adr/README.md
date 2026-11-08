# Architecture Decision Records

**Document:** `SOC-DOC-ADR` · Index of Societix Architecture Decision Records (ADRs).

An **ADR** captures a single significant architectural decision: the context that forced it, the
decision taken, and the consequences we accept. ADRs are **immutable once Accepted** — we don't edit
history; we supersede a decision with a new ADR. The canonical stack in
[`../README.md`](../README.md) is authoritative; these ADRs record *why* the stack is what it is and
must stay consistent with it.

---

## Index

| ADR | Title | Status |
|---|---|---|
| [0001](0001-modular-monolith-spring-modulith.md) | Modular monolith with Spring Modulith | Accepted |
| [0002](0002-postgres-rls-multitenancy.md) | Shared-schema multi-tenancy via PostgreSQL RLS | Accepted |
| [0003](0003-payment-aggregator-no-float.md) | Licensed Payment Aggregator; Societix holds no float | Accepted |
| [0004](0004-kafka-event-driven-cadence.md) | Kafka event backbone with transactional outbox | Accepted |
| [0005](0005-offline-gate-sqlite-crdt.md) | Offline-authoritative gate: SQLite + CRDT | Accepted |
| [0006](0006-crypto-evoting-scheme.md) | Commit-reveal + hash-chained e-voting (not blockchain) | Accepted |
| [0007](0007-maven-supported-version-baseline.md) | Maven build and supported version baseline | Accepted |

## How & when to add an ADR

Write an ADR when a decision is **costly to reverse** or **constrains many future choices** —
a datastore, a tenancy model, a messaging backbone, a crypto scheme, a cross-cutting pattern,
a change to the canonical stack. You do **not** need an ADR for routine, easily-reversed choices
(a library swap inside one module, a naming tweak).

1. Copy the **template** below into `NNNN-short-kebab-title.md` with the next zero-padded number.
2. Draft with **Status: Proposed**; discuss in the PR.
3. On merge with team sign-off, set **Status: Accepted** and add the row to the index table above.
4. To change an accepted decision, write a **new** ADR that supersedes it: set the old one's status
   to `Superseded by ADR-NNNN` and the new one's to `Supersedes ADR-MMMM`. Never rewrite an accepted
   ADR's decision.

Status values: `Proposed` · `Accepted` · `Superseded by ADR-NNNN` · `Deprecated`.

## Template

```markdown
# ADR-NNNN: <Short title of the decision>

**Status:** Proposed | Accepted | Superseded by ADR-NNNN | Deprecated
**Date:** YYYY-MM-DD
**Deciders:** <roles/people>

## Context
What is the problem, the forces at play, the relevant BRD/NFR requirements and constraints?
State facts, not the decision. Link the anchor docs.

## Decision
The decision, stated in one or two clear sentences ("We will …"), followed by the specifics.

## Consequences
What becomes easier and what becomes harder once this is in place.

**Pros**
- …

**Cons / costs**
- …

## Alternatives considered
Each option, why it was viable, and why it was not chosen.
```

---

**Related:** [`../README.md`](../README.md) · [`../01-architecture.md`](../01-architecture.md) ·
[`../standards/coding-standards.md`](../standards/coding-standards.md)
