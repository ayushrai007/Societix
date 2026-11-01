# SOC-31 — Channel fallback and delivery logging

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-31** · component **Platform** · suggested sprint **S3** ·
size **2 point(s)** · priority **Medium**.

## Goal
If the preferred channel fails, escalate (in-app -> WhatsApp -> SMS). Log what was delivered and when (needed for legal proof of meeting notices later). Keep it idempotent so one event = one message.

## Preconditions (must already exist)
SOC-30.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a failed channel escalates to the next
- delivery attempts are logged
- the same event never sends two duplicate messages

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
