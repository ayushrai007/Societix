# SOC-10 — Define the Society (tenant) entity and onboarding

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-10** · component **Platform** · suggested sprint **S0** ·
size **2 point(s)** · priority **Highest**.

## Goal
Create the Society entity (one row per apartment complex) and an admin endpoint to onboard a new society. This is the 'tenant' every other table will belong to. Where to start: entity + migration + repository + a simple create endpoint. See docs/02-how-it-works.md ('one program, one database').

## Preconditions (must already exist)
see prompts/README.md build order — ensure earlier tasks exist.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- can create a society via an admin endpoint
- society has id, name, address, state (for later law rules)
- stored via a Flyway migration

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
