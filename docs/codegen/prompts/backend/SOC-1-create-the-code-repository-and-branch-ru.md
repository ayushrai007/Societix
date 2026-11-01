# SOC-1 — Create the code repository and branch rules

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-1** · component **Platform** · suggested sprint **S0** ·
size **1 point(s)** · priority **Highest**.

## Goal
Set up the Git repository on GitHub with a protected main branch. Add a README that points to docs/. Configure branch protection so every change goes through a Pull Request with one approval and passing checks. Where to start: create the repo, add .gitignore for Java/Node, enable branch protection in repo settings. Fits in: this is the very first task; everything else branches off it.

## Preconditions (must already exist)
see prompts/README.md build order — ensure earlier tasks exist.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- main branch cannot be pushed to directly
- a PR requires 1 approval + green checks before merge
- repo README links to docs/

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
