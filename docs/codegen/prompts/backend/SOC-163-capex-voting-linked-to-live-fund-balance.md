# SOC-163 — Capex voting linked to live fund balances

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-163** · component **Governance** · suggested sprint **S10** ·
size **2 point(s)** · priority **Medium**.

## Goal
When voting on spending from a fund, show the live balance from the Treasury ledger (not a stale copy) so owners vote with real numbers. Depends on M1 ledger (SOC-100).

## Preconditions (must already exist)
SOC-100, SOC-161.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a spending resolution shows the current fund balance from the ledger
- the balance is live, not cached stale
- the resolution result is recorded and audited

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
