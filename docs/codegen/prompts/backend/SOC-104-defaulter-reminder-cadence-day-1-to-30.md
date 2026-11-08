# SOC-104 — Defaulter reminder cadence (Day +1 to +30)

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-104** · component **Treasury** · suggested sprint **W5** ·
size **3 point(s)** · priority **High**.

## Goal
Automatic chasing of unpaid bills: day 1 gentle in-app/WhatsApp nudge, day 10 SMS/call, day 15 late fee added (respecting state-law caps), day 30 a formal notice document. Stops instantly when they pay. Uses the scheduled jobs (SOC-22) and notifications (SOC-30).

## Preconditions (must already exist)
SOC-22, SOC-30, SOC-40, SOC-103.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- an unpaid invoice triggers the right reminder at each stage
- paying mid-cadence cancels remaining reminders
- each stage change writes an event and is audited

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.
- MANDATORY tenancy-isolation test: a new society-owned table must prove society A cannot read society B.
- MANDATORY idempotency test: a repeated call/webhook/event causes no duplicate effect.


## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
