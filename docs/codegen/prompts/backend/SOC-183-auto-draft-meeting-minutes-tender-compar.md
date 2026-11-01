# SOC-183 — Auto-draft meeting minutes + tender comparison

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-183** · component **Copilot** · suggested sprint **S12** ·
size **3 point(s)** · priority **Low**.

## Goal
Two smaller copilot features: turn meeting notes into a formatted draft of minutes, and turn a set of vendor quotes into a comparison table. Where to start: a summarise prompt + a structured-extraction prompt.

## Preconditions (must already exist)
SOC-180.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- notes produce a readable draft of minutes
- multiple vendor quotes produce a comparison table
- outputs are editable by the user before saving

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
