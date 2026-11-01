# SOC-181 — Natural-language financial Q&A (RAG)

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-181** · component **Copilot** · suggested sprint **S11** ·
size **5 point(s)** · priority **Medium**.

> **Hard internals:** for the algorithm/design, read [`this deep-dive`](../../../archive/deep-dives/dd-04-ai-copilot-rag.md) — follow it rather than improvising.

## Goal
Let a committee member ask plain-English questions and get grounded answers with citations, built on retrieval (find relevant data) + generation (AI writes the answer). Must stay within the user's society and answer in under 2.5s (cache repeats). See the archived RAG deep-dive.

## Preconditions (must already exist)
SOC-180.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a NL question returns an answer grounded in the society's own data with citations
- it never surfaces another society's data
- a repeated query returns within 2.5s

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
