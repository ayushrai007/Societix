# SOC-102 — Give each flat a payment number (VAN / UPI VPA)

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-102** · component **Treasury** · suggested sprint **W3** ·
size **3 point(s)** · priority **Highest**.

## Goal
Integrate the payment provider to assign each flat a unique virtual account number and UPI address, so incoming money is auto-identified. Use the mock provider locally. See glossary (VAN/UPI VPA). Pairs with SOC-103.

## Preconditions (must already exist)
SOC-3, SOC-101.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- each flat gets a unique VAN and UPI VPA
- the mapping flat<->VAN is stored
- works against the mock provider locally

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
