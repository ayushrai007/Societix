# SOC-200 — Import engine for Tally / Excel exports

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-200** · component **Migration** · suggested sprint **W5** ·
size **3 point(s)** · priority **Medium**.

## Goal
Build the importer that reads a society's old accounting data (Tally XML / Excel) and loads it into our ledger and member records. Produce a mapping report. See docs/04-the-6-modules.md#m6.

## Preconditions (must already exist)
SOC-10, SOC-100.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a Tally/Excel export is parsed and loaded
- records map to our ledger and member master
- a mapping/summary report is produced

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
