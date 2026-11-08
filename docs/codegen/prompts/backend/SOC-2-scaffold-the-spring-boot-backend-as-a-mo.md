# SOC-2 — Scaffold the Spring Boot backend as a modular monolith

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-2** · component **Platform** · suggested sprint **S0** ·
size **2 point(s)** · priority **Highest**.

## Goal
Create the Java 25 + Spring Boot 4.1.2 backend using Maven 3.10.0 and Maven Wrapper 3.3.4. Add Spring Modulith 2.1.2 and create empty module packages: treasury, governance, copilot, gate, privacy, migration, platform. One runnable app that starts and serves a /health endpoint. Where to start: Spring Initializr, add WebMVC/Validation/Actuator/Modulith deps. See docs/03-tech-choices.md for why these tools.

## Preconditions (must already exist)
SOC-1.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- ./mvnw spring-boot:run starts the app
- GET /actuator/health returns 200
- each module is its own package with a package-info marking the module boundary

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
