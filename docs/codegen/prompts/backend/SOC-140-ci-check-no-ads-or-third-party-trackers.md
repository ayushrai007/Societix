# SOC-140 — CI check: no ads or third-party trackers

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-140** · component **Privacy** · suggested sprint **S2** ·
size **2 point(s)** · priority **Highest**.

## Goal
Add an automated check in the build pipeline that fails if any ad/tracking SDK appears in the apps. This enforces our zero-ads promise forever. Why: brand + DPDP law. See docs/04-the-6-modules.md#m5.

## Preconditions (must already exist)
see prompts/README.md build order — ensure earlier tasks exist.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a known tracker dependency makes the build fail
- the check runs on every PR
- the allowed/blocked list is documented

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
