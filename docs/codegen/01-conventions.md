# 01 — Conventions (the rules code must follow)

**What this is:** the fixed, checkable rules for all Societix code. Every rule is phrased so a
reviewer can answer "did the code do this? yes/no."
**Who should read it:** Claude (as context, every session) and every reviewer.

Where a term is unfamiliar, see [../06-glossary.md](../06-glossary.md).

---

## 1. Stack & versions (do not substitute without an ADR)

| Area | Fixed choice |
|---|---|
| Language | Java 25 (LTS) |
| Framework | Spring Boot 4.1.2 + Spring Modulith 2.1.2 |
| Build | Apache Maven 3.10.0, with Maven Wrapper 3.3.4 committed |
| Test infrastructure | Testcontainers 2.0.5 |
| Database | PostgreSQL 18 + pgvector 0.8.7 (`pgvector/pgvector:0.8.7-pg18`) |
| Migrations | Flyway (`V<n>__<desc>.sql`, applied on startup) |
| Cache/locks | Redis 8.2 (Extended Support) |
| Events | Postgres **outbox table** + scheduled poller (ShedLock). **No Kafka/broker.** |
| Payments | Razorpay/Cashfree aggregator; mock locally. **We never hold funds.** |
| Web | Node.js 26 (LTS) + React 18 + TypeScript + Vite + TanStack Query |
| Mobile | Node.js 26 (LTS) + React Native (Expo) + TypeScript |
| Auth | OIDC login → signed JWT carrying `userId`, `societyId`, `roles` |

## 2. Backend repository layout (package-by-module)

Root package `com.societix`. One package per module; modules talk through public APIs only, never by
reaching into each other's internals.

```
com.societix
├── platform      # shared: config, security, tenancy, outbox, audit, error model
├── treasury      # M1
├── governance    # M2
├── copilot       # M3
├── gate          # M4
├── privacy       # M5
└── migration     # M6
```

Within a module: `api` (controllers + DTOs), `domain` (entities + business logic), `data`
(repositories), `internal` (not exposed to other modules). **Rule:** a controller never touches
another module's entities directly.

## 3. Layering & types

- **Constructor injection only.** No field injection (`@Autowired` on fields is forbidden).
- **Three kinds of object, kept separate:** API **DTOs** (what crosses the HTTP boundary; Java
  `record`s, immutable), **entities** (JPA, persistence), **domain** logic. Never serialize a JPA
  entity directly to the client.
- **Validation** on inbound DTOs via Jakarta Validation annotations; invalid input → 400 with the
  standard error body.

## 4. The tenancy rule (most important — privacy-critical)

- Every table holding a society's data **must** have a non-null `tenant_id` column.
- Every such table **must** have a PostgreSQL Row-Level Security policy filtering by the current
  tenant (set from the JWT into the session variable `app.tenant_id` at the start of each request's
  transaction).
- A query that runs with no tenant set returns **zero rows** (fail-closed), never all rows.
- **Check:** for any new table, the PR must include the RLS policy migration and a test proving
  society A cannot read society B's rows.

## 5. Error model (uniform)

- All error responses use **RFC 9457 `application/problem+json`**: `type`, `title`, `status`,
  `detail`, plus an app `code` and (for validation) a list of field errors.
- One global exception handler maps exceptions → this shape. Controllers don't hand-build error JSON.
- Never leak stack traces, SQL, or PII in error bodies.

## 6. Idempotency

- Payment webhooks and any externally-retried write accept/derive an **idempotency key**; a repeat
  with the same key makes no second change (enforced via a unique constraint and/or a Redis key).
- Outbox handlers are idempotent — processing the same event twice is harmless.

## 7. Logging & privacy

- **No personal data in logs ever:** no phone numbers, emails, names, visitor photos, bank details,
  payment references. Log stable IDs (`unitId`, `invoiceId`) instead.
- Logs are structured (key-value/JSON), not free-text string concatenation.
- Anything that must auto-delete (visitor PII at 30 days) is deleted from primary storage on
  schedule, and the deletion is recorded in the audit log.

## 8. Testing bar

- **Unit tests** for business logic (fast, no DB).
- **Slice/integration tests** using **Testcontainers** (real Postgres/Redis in a container) for
  anything touching the database, RLS, or the outbox.
- Every task's tests **map to its acceptance criteria** — name the mapping.
- **Mandatory special tests:** a tenancy-isolation test (cross-society read returns nothing) for any
  new tenant table; an idempotency test for any webhook/handler; a privacy test where data must
  auto-delete.
- Target meaningful coverage of logic, not a vanity percentage; a task with untested acceptance
  criteria is not done.

## 9. API conventions

- Base path `/api/v1`. Resource names are plural nouns (`/invoices`, `/visitors`).
- Standard verbs/status codes; `201` on create with a `Location`, `204` on empty success, `4xx` for
  client errors with problem+json.
- Cursor or page/size pagination on list endpoints; never return unbounded lists.
- Money as integer **minor units** (paise) or `BigDecimal` — never `float`/`double`.
- Dates/times ISO-8601, UTC in storage.
- OpenAPI 3.1 is generated from controllers and kept current.

## 10. Frontend conventions (web + mobile)

- TypeScript everywhere; no `any` without a written reason.
- API access through a generated/typed client (from the OpenAPI spec), not ad-hoc `fetch` scattered
  around.
- Server state via **TanStack Query** (web); clear loading/error/empty states on every screen.
- No ad or third-party tracking SDKs — **ever** (M5). A dependency check enforces this (SOC-140).
- Gate app is **offline-first**: writes go to the local store first, then sync.

## 11. Git & PR

- Trunk-based: short-lived branches off `master`, named `type/short-desc` (e.g. `feat/soc-13-rls`).
- **Conventional Commits** (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`…).
- Small PRs (ideally one task). PR description links the backlog ID and lists how acceptance criteria
  were verified. One approval + green CI required to merge. No self-merge.

## 12. Security baseline

- Secrets only via environment/secret store; nothing sensitive committed.
- All traffic over TLS; security headers on.
- AuthZ on every endpoint (role-checked); deny by default.
- Dependencies scanned in CI; no known-critical vulnerabilities merged.

---

Each of these is intentionally a yes/no check. If a task would require breaking one, Claude must say
so and ask — see [`00-playbook.md`](00-playbook.md).
