# SOC-201 — Import from MyGate exports

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-201** · component **Migration** · suggested sprint **S10** ·
size **2 point(s)** · priority **Low**.

## Goal
Add an importer for a competitor (MyGate) export - residents, vehicles, visitor history - so switching societies don't re-enter residents. Where to start: a MyGate format adapter reusing the SOC-200 pipeline.

## Preconditions (must already exist)
SOC-200.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a MyGate export imports residents and vehicles
- duplicates are detected, not doubled
- unmapped fields are reported, not silently dropped

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
