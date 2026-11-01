# SOC-6 — Walking skeleton: one feature end-to-end

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-6** · component **Platform** · suggested sprint **S2** ·
size **3 point(s)** · priority **High**.

## Goal
Prove the whole chain works with the thinnest possible slice: an endpoint that creates a dummy invoice, a call to the mock payment provider, and a record of the result in the DB. No real business logic yet. Why: de-risks the architecture before we build real features.

## Preconditions (must already exist)
SOC-2, SOC-3, SOC-5.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- can create a dummy invoice via API
- mock payment webhook updates its status
- the flow has one integration test

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
