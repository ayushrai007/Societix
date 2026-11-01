# SOC-30 — Notification service with channel adapters

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-30** · component **Platform** · suggested sprint **S2** ·
size **3 point(s)** · priority **High**.

## Goal
A service that sends a message to a resident via a channel (in-app, WhatsApp, SMS). Start with in-app + one real channel behind an interface so others slot in. Driven by outbox events. Where to start: an interface + in-app impl + a WhatsApp stub.

## Preconditions (must already exist)
SOC-21.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a module can request 'notify resident X'
- at least in-app + one external channel work
- sending is triggered by an outbox event (decoupled)

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
