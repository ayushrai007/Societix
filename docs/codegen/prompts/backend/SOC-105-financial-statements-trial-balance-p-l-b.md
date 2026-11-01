# SOC-105 — Financial statements (trial balance, P&L, balance sheet)

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-105** · component **Treasury** · suggested sprint **S5** ·
size **3 point(s)** · priority **Medium**.

> **Hard internals:** for the algorithm/design, read [`this deep-dive`](../../../archive/deep-dives/dd-01-double-entry-ledger.md) — follow it rather than improvising.

## Goal
Generate the standard financial reports from the ledger automatically. Where to start: aggregation queries over the ledger; a report endpoint per statement. See the archived ledger deep-dive for the exact math.

## Preconditions (must already exist)
SOC-100.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- trial balance always balances (debits = credits)
- P&L and balance sheet generate for a date range
- reports are read-only and tenant-scoped

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
