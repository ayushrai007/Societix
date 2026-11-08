# SOC-120 — Gate app shell with local on-device storage

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-120** · component **Gate** · suggested sprint **Post-1-Jan** ·
size **3 point(s)** · priority **Highest**.

## Goal
Create the React Native gate tablet app that stores visitor records in a small on-device database first (works with no internet). See docs/04-the-6-modules.md#m4. Pairs with SOC-121 for sync.

## Preconditions (must already exist)
SOC-11, SOC-122 (backend half).
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- the app runs on a tablet and stores a check-in locally
- works with the network switched off
- local records persist across app restarts

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
