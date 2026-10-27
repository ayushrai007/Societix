# ADR-0002: Shared-schema multi-tenancy via PostgreSQL RLS

**Status:** Accepted
**Date:** 2026-10-27
**Deciders:** Principal Engineer, Backend Lead, Security/DPDP Lead

## Context

Societix is multi-tenant: each **society** is a tenant, scaling toward **~440k units** across many
thousands of societies (BRD). Requirements shaping the tenancy model:

- **BRD NFR:** "strict multi-tenant PostgreSQL **RLS** with dedicated [encryption] keys" — isolation
  must be enforced at the **database**, not merely in application code.
- **DPDP Act 2023** data-isolation obligations: a bug in one request must not leak another society's
  personal data (visitor PII, phone numbers, bank identifiers).
- **Cost & operability at scale**: thousands of tenants must not mean thousands of schemas or
  databases to migrate, back up, connection-pool, and monitor.
- The data model already assumes it: every tenant table has a non-null `tenant_id`, RLS policies, and
  per-society envelope keys ([`../03-data-model.md`](../03-data-model.md) §3–4;
  [`../01-architecture.md`](../01-architecture.md) §6).

## Decision

**We will use a single shared schema with row-level multi-tenancy enforced by PostgreSQL
Row-Level Security (RLS)**, keyed on a per-request session GUC **`app.tenant_id`**, combined with
**per-tenant data-encryption keys** (envelope encryption) for sensitive columns.

- Every tenant-owned table carries a non-null `tenant_id` and a `tenant_isolation` RLS policy
  (`USING`/`WITH CHECK` on `tenant_id = current_setting('app.tenant_id')::uuid`), applied via a
  repeatable Flyway migration so **no table ships without a policy**.
- A `OncePerRequestFilter` resolves `tenant_id` from the verified JWT and runs
  `SET LOCAL app.tenant_id = :tid` **inside the request transaction**.
- RLS is `ENABLE` + `FORCE` (so even the table owner is filtered). The application DB role is **not**
  `BYPASSRLS`. A missing/blank GUC makes `current_setting(...)` throw → the query **fails closed**,
  never leaks.
- Sensitive columns (visitor name/phone/photo, vendor/payout bank identifiers) are encrypted with a
  **per-society DEK** wrapped by a KMS master key.

## Consequences

**Pros**
- Isolation enforced by the database — a forgotten `WHERE tenant_id = ?` cannot leak data; the
  default posture is deny.
- One schema to migrate, back up, and pool connections against → operationally cheap at 440k-unit
  scale.
- Per-tenant keys give cryptographic blast-radius containment and support DPDP erasure (destroy a
  tenant's key).
- Fails closed by design; tenancy is testable (an integration test asserts a cross-tenant read
  returns zero rows).

**Cons / costs**
- Every DB-touching use case **must** run inside a transaction so `SET LOCAL` is in scope — a
  discipline enforced by review and tests ([`../standards/coding-standards.md`](../standards/coding-standards.md) §10).
- RLS policies add a small per-query predicate cost; indexes must lead with `tenant_id`.
- A very large "whale" tenant shares tables with small ones (noisy-neighbor); mitigated by per-tenant
  rate limits and, if ever needed, future partitioning/extraction — not a reason to change the model.
- Developers must never run the app under a `BYPASSRLS`/superuser role; one misconfiguration defeats
  isolation (covered by startup assertions and ops runbooks).

## Alternatives considered

- **Schema-per-tenant.** Stronger physical separation, but thousands of schemas make migrations,
  connection pooling, and monitoring painful and slow; `search_path` juggling is error-prone.
  Rejected at this scale.
- **Database-per-tenant.** Strongest isolation; wholly impractical for 440k units (thousands of DBs
  to provision, patch, back up, and fail over). Reserved only as a bespoke option for an exceptional
  enterprise tenant, not the default.
- **Application-only filtering (no RLS).** Cheapest to build but a single missing predicate leaks
  cross-tenant data — unacceptable under DPDP and the BRD NFR. Rejected; RLS is the backstop precisely
  because app code is fallible.

---

**Related:** [`../03-data-model.md`](../03-data-model.md) §3–4 ·
[`../01-architecture.md`](../01-architecture.md) §6 ·
[`../05-security-compliance.md`](../05-security-compliance.md) ·
[`../standards/coding-standards.md`](../standards/coding-standards.md) §10
