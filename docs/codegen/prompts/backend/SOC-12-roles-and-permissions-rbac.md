# SOC-12 — Roles and permissions (RBAC)

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-12** · component **Platform** · suggested sprint **S1** ·
size **2 point(s)** · priority **Highest**.

## Goal
Define roles (Platform Admin, Secretary, Treasurer, Committee Member, Owner, Tenant, Guard, Auditor) and enforce them on endpoints. A resident can't hit a treasurer-only endpoint. Where to start: a role enum + method-level security annotations.

## Preconditions (must already exist)
SOC-11.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- each role is defined
- a treasurer-only endpoint rejects an owner's token (403)
- roles come only from the signed JWT, never request input

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
