# SOC-5 — Database migrations with Flyway

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-5** · component **Platform** · suggested sprint **S1** ·
size **1 point(s)** · priority **High**.

## Goal
Wire in Flyway so database schema changes are version-controlled SQL scripts applied automatically on startup. Add the first migration (an empty baseline). Document the naming convention (V1__description.sql). Why: so every environment has an identical, reproducible schema.

## Preconditions (must already exist)
SOC-2.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- Flyway runs migrations on app startup
- a sample V1 migration applies cleanly
- docs explain how to add a new migration

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
