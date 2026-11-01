# 03 — Prompt Template

**What this is:** the structure of every per-task prompt, plus a worked example. The files in
[`prompts/`](prompts/README.md) all follow this shape — generated from the backlog so they stay in
sync.
**Who should read it:** anyone writing a new prompt or understanding an existing one.

---

## The template

Every task prompt has these sections:

```
# <BACKLOG-ID> — <Task title>

## Context
You are building Societix. Before this task, load these as context:
docs/codegen/00-playbook.md, 01-conventions.md, 02-tech-blueprint.md.
This task is backlog item <ID> (component: <...>, sprint: <...>).
<One or two lines of plain context on where this fits.>

## Goal
<Plain-English statement of what this task must achieve.>

## Preconditions (already built)
<What earlier tasks must exist first, by ID. If a precondition is missing, stop and say so.>

## Deliverables (exact)
<Concrete list: files to add/change; entities + columns; endpoints (method, path, request,
response); migrations; UI screens — whatever applies. Be specific enough to be unambiguous.>

## Acceptance criteria (must all pass)
<The backlog item's acceptance criteria, each testable.>

## Tests to write
<Which tests, mapped to each acceptance criterion; which need Testcontainers; the mandatory
tenancy/idempotency/privacy test if applicable.>

## Out of scope
<What NOT to build here — pushes against scope creep into later tasks.>

## Self-review before you finish
Run the checklist in 00-playbook.md. Confirm tenancy, no-PII-logging, idempotency, error model,
and tests-pass. State honestly anything incomplete or assumed.
```

## Worked example (SOC-13)

```
# SOC-13 — Data isolation with Row-Level Security (RLS)

## Context
You are building Societix. Load docs/codegen/00-playbook.md, 01-conventions.md, 02-tech-blueprint.md
first. This is backlog item SOC-13 (component: Platform, sprint: S1). This is the privacy-critical
foundation: the database itself must refuse to show one society's data to another.

## Goal
Make every society-owned table enforce tenant isolation via PostgreSQL Row-Level Security, driven by
the tenant id from the request's JWT — so even a buggy query cannot leak another society's rows.

## Preconditions (already built)
- SOC-2 (backend scaffold), SOC-5 (Flyway), SOC-10 (Society entity), SOC-11 (JWT with societyId).
  If any is missing, stop and say so.

## Deliverables (exact)
- A `TenantContext` (request-scoped) populated from the JWT's societyId by a security filter.
- A transaction step that runs `SET LOCAL app.tenant_id = :societyId` at the start of each request
  transaction (per 02-tech-blueprint §6).
- A Flyway migration that, for each existing tenant table, adds `tenant_id uuid not null`, enables
  RLS, and creates a policy `USING (tenant_id = current_setting('app.tenant_id')::uuid)`.
- A documented helper so future tasks apply the same policy to new tables.

## Acceptance criteria (must all pass)
- A request acting for society A returns zero rows belonging to society B.
- A query with no app.tenant_id set returns zero rows (fail-closed), not all rows.
- There is an automated test proving cross-society reads are blocked.

## Tests to write
- Testcontainers integration test: seed two societies' rows; assert a query under A sees only A's;
  assert no-tenant-set sees none. This is the mandatory tenancy-isolation test.

## Out of scope
- Per-tenant encryption keys (later). Only RLS isolation here.

## Self-review before you finish
Run the 00-playbook checklist; confirm fail-closed behavior and the isolation test passes.
```

## Notes

- Prompts are **starting points** — if Claude needs a decision the prompt doesn't cover, it must ask
  (playbook rule 10).
- For hard Phase-2 internals (e-voting SOC-162, offline CRDT SOC-121, RAG SOC-181), the prompt points
  Claude at the matching deep-dive in [`../archive/deep-dives/`](../archive/README.md) for the
  algorithm, rather than restating it.
