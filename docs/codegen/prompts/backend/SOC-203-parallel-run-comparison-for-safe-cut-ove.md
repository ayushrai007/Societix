# SOC-203 — Parallel-run comparison for safe cut-over

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-203** · component **Migration** · suggested sprint **S11** ·
size **2 point(s)** · priority **Low**.

## Goal
Let a society run old + Societix side-by-side for a period and compare outputs before switching fully, so cut-over is zero-risk. Where to start: a daily comparison report of the two systems' key figures.

## Preconditions (must already exist)
SOC-200.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- both systems can process the same period
- a comparison report highlights differences
- cut-over is gated on a clean comparison

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
