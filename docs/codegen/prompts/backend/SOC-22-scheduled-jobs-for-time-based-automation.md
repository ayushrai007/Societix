# SOC-22 — Scheduled jobs for time-based automation

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-22** · component **Platform** · suggested sprint **S2** ·
size **2 point(s)** · priority **High**.

## Goal
Add a scheduling mechanism for 'do X after N days' needs (defaulter reminders, 30-day data purge). A daily job queries rows that are now due. Where to start: a simple @Scheduled job + a query-due-rows pattern reused by M1 and M5.

## Preconditions (must already exist)
SOC-21.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a scheduled job can find rows due today
- the mechanism is reusable by Treasury and Privacy
- runs once per schedule even with multiple instances

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
