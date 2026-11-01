# SOC-106 — Maker-checker vendor payouts with tax tracking

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-106** · component **Treasury** · suggested sprint **S6** ·
size **3 point(s)** · priority **Medium**.

## Goal
Pay vendors safely: one user requests a payout, a different user approves it before it executes. Track TDS (tax withheld) and GST input credit. See glossary (maker-checker, TDS/GST).

## Preconditions (must already exist)
SOC-100, SOC-12.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a payout needs a different approver than the requester
- TDS and GST are recorded on the payout
- an unapproved payout cannot execute

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
