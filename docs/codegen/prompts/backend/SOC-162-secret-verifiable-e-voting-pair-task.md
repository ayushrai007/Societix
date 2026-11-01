# SOC-162 — Secret, verifiable e-voting (PAIR TASK)

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-162** · component **Governance** · suggested sprint **S9** ·
size **5 point(s)** · priority **High**.

> **Hard internals:** for the algorithm/design, read [`this deep-dive`](../../../archive/deep-dives/dd-03-crypto-evoting.md) — follow it rather than improvising.

## Goal
The hardest governance piece: online ballots that are secret (no one sees how you voted) yet verifiable (the count can be proven correct and not tampered with). Build as a PAIR. Uses a commit-reveal scheme + the hash-chained audit log - see the archived e-voting deep-dive. Don't start this in a phase's last sprint.

## Preconditions (must already exist)
SOC-40, SOC-161.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- an eligible owner casts exactly one weighted vote per unit
- the tally is verifiable without revealing individual votes
- duplicate or ineligible votes are rejected and logged

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
