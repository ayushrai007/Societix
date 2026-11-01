# SOC-21 — Background job that drains the outbox

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-21** · component **Platform** · suggested sprint **S2** ·
size **3 point(s)** · priority **Highest**.

## Goal
Add a background job that runs every few seconds, reads unprocessed outbox rows, and calls the matching handler, then marks them processed. Guard it so only one app instance runs it at a time (ShedLock). Why: this is what makes 'automatic' happen. Handlers must be safe to run twice (idempotent).

## Preconditions (must already exist)
SOC-20.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- unprocessed events are dispatched within seconds
- only one instance processes at a time
- a handler running twice has the same effect as once

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.

- MANDATORY idempotency test: a repeated call/webhook/event causes no duplicate effect.


## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
