# SOC-103 — Zero-touch payment matching (PAIR TASK)

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-103** · component **Treasury** · suggested sprint **S4** ·
size **5 point(s)** · priority **Highest**.

## Goal
The headline feature. When the payment provider sends a 'money received' webhook, match it to the flat by its VAN/VPA, mark the invoice paid, and post it to the ledger - all automatically, no manual matching. Build this as a PAIR (Dev A + Dev B); it's the trickiest money path. Must be idempotent (a repeated webhook doesn't double-post).

## Preconditions (must already exist)
SOC-101, SOC-102, SOC-20.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a settlement webhook marks the right invoice paid and posts to the ledger
- a duplicate webhook causes no second posting
- a payment matching no invoice goes to an 'unmatched' queue, never lost

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.
- MANDATORY tenancy-isolation test: a new society-owned table must prove society A cannot read society B.
- MANDATORY idempotency test: a repeated call/webhook/event causes no duplicate effect.


## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
