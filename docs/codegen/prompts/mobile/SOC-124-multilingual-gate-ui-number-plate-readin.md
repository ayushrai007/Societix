# SOC-124 — Multilingual gate UI + number-plate reading (ANPR)

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-124** · component **Gate** · suggested sprint **S6** ·
size **3 point(s)** · priority **Low**.

## Goal
Localise the guard UI into the major Indian languages and add automatic vehicle number-plate reading for known vehicles. ANPR can use a third-party library/service. Where to start: i18n framework + an ANPR adapter interface.

## Preconditions (must already exist)
SOC-120.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- guard UI available in at least 3 languages
- a known vehicle plate is recognised and logged
- an unknown plate falls back to manual entry

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
