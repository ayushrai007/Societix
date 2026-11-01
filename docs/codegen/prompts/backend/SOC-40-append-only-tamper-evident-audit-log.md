# SOC-40 — Append-only, tamper-evident audit log

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-40** · component **Platform** · suggested sprint **S1** ·
size **3 point(s)** · priority **High**.

## Goal
A write-once audit table recording sensitive actions (money moves, votes, data deletions). Rows can't be edited or deleted; each links to the previous via a hash so tampering is detectable. Auditors read it. Why: trust + legal. See glossary (audit / hash-chain).

## Preconditions (must already exist)
see prompts/README.md build order — ensure earlier tasks exist.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- sensitive actions write an audit row
- UPDATE/DELETE on the table is rejected
- the hash chain can be verified end-to-end

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
