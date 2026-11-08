# SOC-180 — Set up pgvector and the knowledge index

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-180** · component **Copilot** · suggested sprint **W5** ·
size **3 point(s)** · priority **Medium**.

> **Hard internals:** for the algorithm/design, read [`this deep-dive`](../../../archive/deep-dives/dd-04-ai-copilot-rag.md) — follow it rather than improvising.

## Goal
Enable the pgvector extension and build the index of searchable society knowledge (bylaws, state-law rules, ledger summaries) as embeddings. This is what the AI searches. See docs/04-the-6-modules.md#m3 and glossary (RAG/embedding).

## Preconditions (must already exist)
SOC-5, SOC-13.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- pgvector is enabled via migration
- society documents can be embedded and stored
- search returns the most relevant chunks for a query, tenant-scoped

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
