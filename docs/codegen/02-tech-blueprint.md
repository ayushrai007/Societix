# 02 — Tech Blueprint (the skeleton every task plugs into)

**What this is:** the concrete technical baseline — folder trees, build files, and the core patterns
— so that code from different tasks composes instead of conflicting.
**Who should read it:** Claude (context every session) + anyone setting up the repo.

This is the *target shape*. The first few backlog tasks (SOC-1…SOC-6) actually create it; every later
task builds inside it.

---

## 1. Top-level repository layout

```
societix/
├── backend/            # Java + Spring Boot (the modular monolith)
├── web/                # React + TypeScript + Vite (committee dashboard)
├── mobile/             # React Native (Expo): resident app + gate app
├── docs/               # (this documentation)
├── docker-compose.yml  # local Postgres + Redis + payment mock
└── .github/workflows/  # CI
```

## 2. Backend layout

```
backend/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew / gradlew.bat / gradle/      # committed wrapper
└── src/
    ├── main/java/com/societix/
    │   ├── SocietixApplication.java
    │   ├── platform/            # shared foundations (see §4–§7)
    │   │   ├── config/
    │   │   ├── security/        # JWT, roles, request auth
    │   │   ├── tenancy/         # RLS filter, tenant context
    │   │   ├── outbox/          # outbox entity, writer, poller
    │   │   ├── audit/           # append-only audit log
    │   │   └── error/           # global problem+json handler
    │   ├── treasury/  governance/  copilot/  gate/  privacy/  migration/
    │   │     ├── api/            # controllers + DTO records
    │   │     ├── domain/         # entities + logic
    │   │     ├── data/           # repositories
    │   │     └── internal/
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/        # Flyway V1__*.sql, V2__*.sql, ...
    └── test/ + integrationTest/ # unit + Testcontainers tests
```

## 3. `build.gradle.kts` baseline (shape, not exhaustive)

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.3.+"
    id("io.spring.dependency-management") version "1.1.+"
    id("org.flywaydb.flyway") version "10.+"
}
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("net.javacrumbs.shedlock:shedlock-spring")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}
```

## 4. Local infra — `docker-compose.yml` (no broker)

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment: { POSTGRES_DB: societix, POSTGRES_USER: societix, POSTGRES_PASSWORD: societix }
    ports: ["5432:5432"]
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  pa-sandbox:                      # payment-aggregator mock (Razorpay/Cashfree stand-in)
    image: mockserver/mockserver:5.15.0
    ports: ["1080:1080"]
# NOTE: no Kafka — the event backbone is the Postgres outbox (see ../03-tech-choices.md).
```

## 5. The outbox pattern (our event backbone)

The mechanism that makes things happen automatically, with zero extra infrastructure.

```
outbox table:
  id (uuid) | tenant_id | event_type (text) | payload (jsonb)
  | created_at | processed_at (null until done) | attempts (int)
```

- **Producers** insert an outbox row **in the same transaction** as the business change (e.g. posting
  a ledger entry) — so the event and the change commit together or not at all.
- A **poller** (`@Scheduled`, every few seconds, wrapped in **ShedLock** so only one instance runs it)
  reads rows where `processed_at IS NULL`, dispatches each to its handler, marks it processed, and
  bumps `attempts` on failure for retry/backoff.
- Handlers are **idempotent**. Event names are the stable contract (`billing.invoice.raised`,
  `payment.settled`, `defaulter.stage.advanced`, `gate.visit.logged`, `privacy.purge.due`,
  `audit.action.recorded`, …).
- Time-based jobs ("do X after N days") are separate `@Scheduled` jobs querying due rows.

## 6. The tenancy filter (RLS) — how a request becomes tenant-scoped

```
1. A request arrives with a JWT (userId, societyId, roles).
2. A security filter validates the JWT and puts societyId into a request-scoped TenantContext.
3. A transaction-aware step runs `SET LOCAL app.tenant_id = :societyId` at the start of the
   request's DB transaction.
4. Every tenant table has an RLS policy: USING (tenant_id = current_setting('app.tenant_id')::uuid).
5. Result: the database itself only returns the current society's rows. No tenant set → no rows.
```

Every tenant table's Flyway migration ships **with** its `ENABLE ROW LEVEL SECURITY` + `CREATE POLICY`.

## 7. Auth / JWT shape

- Login (OIDC + phone OTP for first contact) issues a short-lived signed JWT.
- Claims: `sub` (userId), `societyId`, `roles` (array), `exp`.
- A Spring Security filter validates the token, builds the authentication, and populates
  `TenantContext`. Endpoints are guarded by role (`@PreAuthorize` or equivalent). Deny by default.

## 8. Web app layout

```
web/
├── package.json  vite.config.ts  tsconfig.json
└── src/
    ├── main.tsx  App.tsx
    ├── api/          # typed client generated from OpenAPI + TanStack Query hooks
    ├── auth/         # login, token storage, route guards
    ├── features/     # one folder per area: treasury/, governance/, gate/, ...
    ├── components/   # shared UI (navy #0f1c3f + emerald #0f9d6b tokens)
    └── lib/
```

## 9. Mobile app layout (Expo)

```
mobile/
├── app.json  package.json  tsconfig.json
└── src/
    ├── resident/     # resident app screens: pay, visitors, voting, tickets
    ├── gate/         # guard app: offline-first visitor check-in
    │   └── offline/  # local SQLite store + sync layer (CRDT merge on reconnect)
    ├── api/          # typed client + auth
    └── shared/
```

## 10. Config & environments

- All config via environment variables (12-factor). `application.yml` reads env with local defaults.
- Local: `docker compose up -d` then `./gradlew bootRun`.
- Environments: `dev` → `staging` → `prod`, single region (Mumbai) to start, managed Postgres/Redis.
- Secrets from the environment/secret store only — never in the repo.

---

Anything a task needs that isn't here (a specific library choice, a fee rule) is a decision to raise
with the human, per [`00-playbook.md`](00-playbook.md), not to invent silently.
