# SOC-14 — Flat/Unit and Member master records

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-14** · component **Platform** · suggested sprint **W2** ·
size **2 point(s)** · priority **Highest**.

## Goal
Create and manage flat/unit records and resident member mappings (owners and tenants) per society, tenant-scoped under PostgreSQL Row-Level Security (RLS). Required structural master data for billing, VAN assignment, and accounting migration.

## Preconditions (must already exist)
SOC-10, SOC-13.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- can create, update, and list flats/units with unit number, wing, floor, and carpet area
- can associate resident members (owners and tenants) with flats
- all flat and member records are strictly tenant-scoped under RLS

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
