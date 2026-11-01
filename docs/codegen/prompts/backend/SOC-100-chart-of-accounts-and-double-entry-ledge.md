# SOC-100 — Chart of accounts and double-entry ledger tables

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-100** · component **Treasury** · suggested sprint **S3** ·
size **3 point(s)** · priority **Highest**.

> **Hard internals:** for the algorithm/design, read [`this deep-dive`](../../../archive/deep-dives/dd-01-double-entry-ledger.md) — follow it rather than improvising.

## Goal
Create the accounting foundation: account types and a ledger where every transaction records matching debits and credits (so books always balance). Include the statutory fund accounts (sinking/repair/reserve/maintenance). See docs/04-the-6-modules.md#m1 and the archived ledger deep-dive.

## Preconditions (must already exist)
SOC-5, SOC-13.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- ledger tables exist via migration
- a posting must have equal debits and credits or it is rejected
- the standard fund accounts are seeded

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
