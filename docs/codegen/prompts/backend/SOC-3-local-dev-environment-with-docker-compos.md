# SOC-3 — Local dev environment with Docker Compose

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-3** · component **Platform** · suggested sprint **S0** ·
size **2 point(s)** · priority **Highest**.

## Goal
Write docker-compose.yml that starts PostgreSQL 18 with pgvector 0.8.7 (`pgvector/pgvector:0.8.7-pg18`) and Redis 8.2 (`redis:8.2-alpine`) locally, plus a mock payment provider. Add a sample .env and document the commands in docs/01-start-here.md. No message broker (we use the outbox). Where to start: copy the compose snippet from the archived getting-started doc and trim Kafka.

## Preconditions (must already exist)
SOC-2.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- docker compose up -d starts Postgres + Redis + payment mock
- app connects to the local DB
- a new dev can go from clone to running app following docs/01-start-here.md

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
