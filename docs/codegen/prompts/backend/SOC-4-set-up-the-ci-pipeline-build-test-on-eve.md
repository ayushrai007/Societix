# SOC-4 — Set up the CI pipeline (build + test on every PR)

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-4** · component **Platform** · suggested sprint **S0** ·
size **2 point(s)** · priority **High**.

## Goal
Add a GitHub Actions workflow that builds the backend, runs tests, and runs static checks on every PR. Add a secret/dependency scan step. Keep it fast. Where to start: a basic Maven verify workflow, then add scans.

## Preconditions (must already exist)
SOC-2.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- every PR triggers build + test
- a failing test blocks merge
- a secret-scanning step runs

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
