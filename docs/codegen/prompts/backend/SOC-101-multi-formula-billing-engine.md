# SOC-101 — Multi-formula billing engine

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-101** · component **Treasury** · suggested sprint **W3** ·
size **3 point(s)** · priority **Highest**.

## Goal
Generate monthly invoices for each flat. Support the common formulas: flat rate, by carpet area (per sq.ft), by metered usage (water), or a mix. Where to start: a BillingFormula strategy per type + an invoice generator job.

## Preconditions (must already exist)
SOC-14, SOC-20, SOC-100.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- can generate invoices for all flats in a society
- supports flat-rate and per-sq-ft at minimum
- raising an invoice writes a billing.invoice.raised event

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
