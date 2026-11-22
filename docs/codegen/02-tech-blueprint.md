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
├── pom.xml                              # Maven parent/build configuration
├── mvnw / mvnw.cmd / .mvn/wrapper/      # committed Maven Wrapper
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
    └── test/java/               # tests run by the configured Maven test lifecycle
```

## 3. `pom.xml` baseline (shape, not exhaustive)

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.1.2</version>
  <relativePath/>
</parent>
<properties>
  <java.version>25</java.version>
  <spring-modulith.version>2.1.2</spring-modulith.version>
  <testcontainers.version>2.0.5</testcontainers.version>
</properties>
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.modulith</groupId>
      <artifactId>spring-modulith-bom</artifactId>
      <version>${spring-modulith.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-bom</artifactId>
      <version>${testcontainers.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webmvc</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
  <dependency><groupId>org.springframework.modulith</groupId><artifactId>spring-modulith-starter-core</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-flyway</artifactId></dependency>
  <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
  <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
  <dependency><groupId>org.testcontainers</groupId><artifactId>testcontainers-postgresql</artifactId><scope>test</scope></dependency>
  <dependency><groupId>org.testcontainers</groupId><artifactId>testcontainers-junit-jupiter</artifactId><scope>test</scope></dependency>
</dependencies>
<build>
  <plugins>
    <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
    <plugin><groupId>org.flywaydb</groupId><artifactId>flyway-maven-plugin</artifactId></plugin>
  </plugins>
</build>
```

## 4. Local infra — `docker-compose.yml` (no broker)

```yaml
services:
  postgres:
    image: pgvector/pgvector:0.8.7-pg18
    environment: { POSTGRES_DB: societix, POSTGRES_USER: societix, POSTGRES_PASSWORD: societix }
    ports: ["5432:5432"]
  redis:
    image: redis:8.2-alpine
    ports: ["6379:6379"]
  pa-sandbox:                      # payment-aggregator mock (Razorpay/Cashfree stand-in)
    image: mockserver/mockserver:9.2.0
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
1. A request arrives with a Firebase ID token (Firebase `sub` plus Societix custom claims).
2. Spring Security validates its signature, issuer, audience, and expiry. Firebase's reserved `sub`
   remains the Firebase UID; Societix claims are `userId`, `tenantId`, `roles`, and
   `authorizationVersion`.
3. The backend checks the current account status and compares `authorizationVersion` with Societix's
   current identity record. A stale token is rejected and the client is told to refresh it.
4. A security filter puts the validated `tenantId` into a request-scoped TenantContext.
5. A transaction-aware step runs `SET LOCAL app.tenant_id = :tenantId` at the start of the
   request's DB transaction.
6. Every tenant table has an RLS policy: USING (tenant_id = current_setting('app.tenant_id')::uuid).
7. Result: the database itself only returns the current society's rows. No tenant set → no rows.
```

Every tenant table's Flyway migration ships **with** its `ENABLE ROW LEVEL SECURITY` + `CREATE POLICY`.

## 7. Auth / JWT shape

- Firebase Phone Auth handles OTP verification and issues the Firebase ID token. Societix accepts
  that token directly as the API bearer token; it does not exchange it for a second token.
- Societix custom claims are `userId`, `tenantId`, `roles` (array), and `authorizationVersion`;
  only the trusted backend writes these, after approval and role assignment.
- A Spring Security resource-server filter validates the token and creates authorities only from the
  signed `roles` claim. Endpoints are guarded by role (`@PreAuthorize` or equivalent); deny by default.
- Society assignment, approval, and current account status remain in Societix's database. Never
  trust role or tenant values supplied in request headers or bodies.
- Custom-claim changes are written through the Postgres outbox. The client refreshes its Firebase
  ID token after Societix confirms claim synchronization; stale `authorizationVersion` values are
  rejected. Only a restricted operator bootstrap can create the first `PLATFORM_ADMIN`.

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
- Local: `docker compose up -d` then `cd backend && ./mvnw spring-boot:run`.
- Environments: `dev` → `staging` → `prod`, single region (Mumbai) to start, managed Postgres/Redis.
- Secrets from the environment/secret store only — never in the repo.

---

Anything a task needs that isn't here (a specific library choice, a fee rule) is a decision to raise
with the human, per [`00-playbook.md`](00-playbook.md), not to invent silently.
