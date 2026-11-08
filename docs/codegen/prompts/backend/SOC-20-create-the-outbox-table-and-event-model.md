# SOC-20 — Create the outbox table and event model

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-20** · component **Platform** · suggested sprint **W1** ·
size **2 point(s)** · priority **Highest**.

## Goal
Add the 'outbox' table: a simple list of 'things that happened' (event type, payload, tenant, created time, processed flag). Producers insert a row in the SAME database save as their change. Why: reliable automatic behaviour with no Kafka. See docs/02-how-it-works.md ('the outbox').

## Preconditions (must already exist)
SOC-5, SOC-10.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- outbox table exists via migration
- a helper lets any module write an event inside its transaction
- events carry the society id

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
