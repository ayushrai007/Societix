# SOC-161 — Live quorum engine

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-161** · component **Governance** · suggested sprint **S8** ·
size **3 point(s)** · priority **High**.

## Goal
During a meeting, track attendance and voting weight live (owners vs validated proxies) against the legal threshold, so the committee knows when voting can start. State laws set the weights. See glossary (quorum/proxy).

## Preconditions (must already exist)
SOC-160.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- attendance updates the quorum count live
- owners and proxies are weighted correctly
- the UI shows 'quorum met' when the threshold is reached

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
