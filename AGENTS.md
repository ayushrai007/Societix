# Societix — Agent Instructions

## 1. Project Overview
Societix is a privacy-first, autonomous residential community operating system for Indian housing
societies (apartment complexes). It handles automated maintenance billing, zero-touch payment
reconciliation, offline-authoritative gate security, secret cryptographic e-voting, and AI-assisted
bylaw/financial copilot workflows.

Key documentation anchors:
- Quickstart & local setup: [`docs/standards/getting-started.md`](docs/standards/getting-started.md)
- Codegen playbook: [`docs/codegen/00-playbook.md`](docs/codegen/00-playbook.md)
- Engineering conventions: [`docs/codegen/01-conventions.md`](docs/codegen/01-conventions.md)
- Technical blueprint: [`docs/codegen/02-tech-blueprint.md`](docs/codegen/02-tech-blueprint.md)
- Delivery plan & assignments: [`docs/05-building-it.md`](docs/05-building-it.md)
- Backlog tasks: [`docs/jira/backlog.md`](docs/jira/backlog.md)

---

## 2. Fixed Stack & Versions (Canonical Baseline)

Per [ADR-0007](docs/adr/0007-maven-supported-version-baseline.md) and [ADR-0008](docs/adr/0008-postgres-outbox-no-broker.md):

| Layer | Component & Version | Notes |
|---|---|---|
| **Runtime** | Java 25 (Temurin LTS) | Pinned LTS baseline |
| **Framework** | Spring Boot 4.1.2 | Starter: `spring-boot-starter-webmvc` |
| **Architecture** | Spring Modulith 2.1.2 | Modular monolith package boundaries |
| **Build Tool** | Apache Maven 3.10.0 | **Always use wrapper:** `backend/mvnw` |
| **Wrapper** | Maven Wrapper 3.3.4 | Script type: `only-script` |
| **Testing** | Testcontainers 2.0.5 | Modules: `testcontainers-postgresql`, `testcontainers-junit-jupiter` |
| **Database** | PostgreSQL 18 + pgvector 0.8.7 | Image: `pgvector/pgvector:0.8.7-pg18` |
| **Cache & Keys** | Redis 8.2 (Extended Support) | Image: `redis:8.2-alpine` |
| **Event Backbone**| PostgreSQL Transactional Outbox | `outbox` table + ShedLock poller (no broker in v1) |
| **Front-End** | Node.js 26 (LTS), React 18, Vite | Mobile/web surfaces follow post-1-Jan-2027 |

---

## 3. Build, Run & Test Commands

All backend commands run from the `backend/` directory:

```bash
cd backend

# Run the backend locally (starts on http://localhost:8080)
./mvnw spring-boot:run

# Verify health endpoints
curl -s http://localhost:8080/health
curl -s http://localhost:8080/actuator/health

# Run unit tests and Modulith architecture verification
./mvnw test

# Full verification: unit tests + integration tests + packaging
./mvnw verify
```

---

## 4. Non-Negotiables

1. **Multi-Tenancy & Data Isolation (Fail-Closed):**
   - Every tenant table has a `tenant_id` column with PostgreSQL Row-Level Security (`RLS`) enabled and forced.
   - Every database use case must execute inside a transaction where `SET LOCAL app.tenant_id = :tid` is set.
   - If `app.tenant_id` is missing or invalid, queries must fail closed (zero rows returned).
   - Every PR touching tenant data must include a cross-tenant integration test (`*IT.java`) asserting that tenant A cannot read or write tenant B rows.
2. **Money Paths & Consistency:**
   - Double-entry ledger: every ledger journal entry must balance (total debits == total credits).
   - Societix holds no float (licensed Payment Aggregator model per ADR-0003).
   - Zero dual-writes: business state and domain events are written to the `outbox` table in the *same* database transaction (ADR-0008).
   - All event consumers and webhooks must be strictly idempotent (Redis idempotency keys + DB unique constraints).
3. **No Secrets in Repo:**
   - Never commit API keys, private keys, or passwords.
   - `.env` and `.env.*` are ignored by git; `.env.example` serves as the tracked template.
4. **Tests with Every Change:**
   - No code merges without accompanying unit tests (`*Test.java`) or Testcontainers integration tests (`*IT.java`).
   - Spring Modulith boundaries are verified via `ApplicationModules.verify()`.

---

## 5. Branch & PR Rules

- **Trunk:** `master` is the only long-lived branch and is always releasable.
- **Branch Naming:** Short-lived feature branches cut from `master`: `<type>/<ticket>-<short-slug>` (e.g. `feat/SOC-10-tenant-entity`).
- **One Task per PR:** Strictly scope each PR to its single backlog ticket. Keep changes small (< 400 lines).
- **Code Review:** Every PR requires at least one approving teammate review (two for money paths, security/auth, RLS, or database migrations).
- **No Self-Merge:** No developer merges their own PR.
- **Squash & Merge:** PRs are squash-merged into `master` with Conventional Commit messages (`feat: ...`, `fix: ...`, `docs: ...`).
