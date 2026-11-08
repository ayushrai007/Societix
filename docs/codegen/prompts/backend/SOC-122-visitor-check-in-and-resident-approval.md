# SOC-122 — Visitor check-in and resident approval

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-122** · component **Gate** · suggested sprint **W3** ·
size **3 point(s)** · priority **High**.

## Goal
Core gate flow: guard logs a visitor, resident gets a notification to approve/deny. Must complete the local check-in in under 1.5 seconds even offline.

## Preconditions (must already exist)
SOC-12, SOC-13, SOC-30 (backend half); SOC-120, SOC-121 (tablet half).
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- guard can log a visitor in under 1.5s
- resident receives an approve/deny notification when online
- visitor record is stored and later synced

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
