# SOC-182 — Safe natural-language to data queries

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-182** · component **Copilot** · suggested sprint **S12** ·
size **3 point(s)** · priority **Medium**.

## Goal
Make the NL->data path safe: never run raw AI-generated SQL on production. Use an allow-listed, parameterised, tenant-scoped query layer. Build as a pair with SOC-181. Why: an AI writing arbitrary SQL is a security hole.

## Preconditions (must already exist)
SOC-181.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- AI cannot execute arbitrary SQL
- all generated queries are validated + parameterised + tenant-scoped
- an injection attempt is blocked and logged

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
