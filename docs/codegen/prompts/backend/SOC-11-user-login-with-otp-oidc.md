# SOC-11 — User login with OTP + OIDC

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-11** · component **Platform** · suggested sprint **S1** ·
size **3 point(s)** · priority **Highest**.

## Goal
Let a user log in. Phone OTP for first contact, bound to a standard OIDC identity, issuing a signed JWT that carries the user id, their society, and their roles. Why OIDC/JWT: standard, secure, we don't invent our own. See glossary for JWT/OIDC.

## Preconditions (must already exist)
SOC-2, SOC-10.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a user can log in and receive a JWT
- the JWT contains society id + roles
- expired/invalid tokens are rejected

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
