# SOC-121 — Offline sync with conflict-free merge (CRDT)

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-121** · component **Gate** · suggested sprint **S4** ·
size **5 point(s)** · priority **High**.

> **Hard internals:** for the algorithm/design, read [`this deep-dive`](../../../archive/deep-dives/dd-02-offline-gate-crdt.md) — follow it rather than improvising.

## Goal
When the internet returns, the tablet syncs local changes up and server changes down, merging automatically without conflicts (CRDT). The server ingest is idempotent. See the archived offline-gate deep-dive for the technique; most of this task is wiring the sync layer.

## Preconditions (must already exist)
SOC-120.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- after a long offline period, all local check-ins reach the server with zero loss
- re-syncing the same records doesn't duplicate them
- a merge conflict resolves deterministically

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.
- MANDATORY tenancy-isolation test: a new society-owned table must prove society A cannot read society B.



## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
