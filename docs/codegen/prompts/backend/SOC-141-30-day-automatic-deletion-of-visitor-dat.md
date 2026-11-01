# SOC-141 — 30-day automatic deletion of visitor data

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-141** · component **Privacy** · suggested sprint **S7** ·
size **3 point(s)** · priority **High**.

## Goal
Visitor photos and phone numbers must be hard-deleted 30 days after the visit. Driven by the scheduled jobs. The deletion itself is audited (so we can prove it happened). See glossary (DPDP).

## Preconditions (must already exist)
SOC-22, SOC-40.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- visitor PII is deleted 30 days after the visit
- deletion covers primary data (and is documented for backups)
- each deletion writes an audit entry

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.


- MANDATORY privacy test: data that must auto-delete is actually gone; no PII in logs.

## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
