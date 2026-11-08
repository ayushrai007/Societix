# SOC-41 — Health checks and basic metrics

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-41** · component **Platform** · suggested sprint **W2** ·
size **2 point(s)** · priority **Medium**.

## Goal
Expose health and basic metrics via Spring Boot Actuator and structured logging. Enough to see if the app is up and spot errors. Full dashboards (Grafana) come later. Why: we need visibility without heavy tooling now.

## Preconditions (must already exist)
SOC-2.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- /actuator/health and basic metrics are exposed
- logs are structured (not plain text) and contain no personal data
- an error is visible in logs/metrics

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
