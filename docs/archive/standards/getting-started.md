# Getting Started — Local Development

**Document:** `SOC-DOC-GS` · **Status:** Standard (prescriptive)

This guide takes a new engineer from an empty machine to a **running Societix stack** —
backend, local infra, and web console — with a seeded demo society. Follow it top to bottom;
every command is copy-pasteable. Architecture context: [`../01-architecture.md`](../01-architecture.md).
Data model & RLS: [`../03-data-model.md`](../03-data-model.md). Terms: [`../glossary.md`](../glossary.md).

> **TL;DR** — `docker compose up -d` → `./gradlew flywayMigrate seedDemo bootRun` →
> `cd web && npm i && npm run dev` → log in as `secretary@maple.demo` / `Password123!`.

---

## 1. Prerequisites

Install these **exact** major versions. Others may work but CI pins these.

| Tool | Version | Install (macOS) | Verify |
|---|---|---|---|
| JDK | **Temurin 21 (LTS)** | `brew install --cask temurin@21` | `java -version` → `21.x` |
| Gradle | **use the wrapper** (`./gradlew`, 8.x) | ships in repo — do **not** install globally | `./gradlew --version` |
| Docker Desktop | latest; ≥ 6 GB RAM allocated | `brew install --cask docker` | `docker info` |
| Node.js | **20 LTS** | `brew install node@20` | `node -v` → `v20.x` |
| Git | ≥ 2.40 | `brew install git` | `git --version` |

> Set `JAVA_HOME` to the Temurin 21 path, e.g.
> `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` (add to your shell profile).
> On Linux/Windows install the equivalent Temurin 21 JDK and Docker Engine; the Gradle wrapper and
> `docker compose` commands are identical.

## 2. Clone

```bash
git clone git@github.com:societix/societix.git
cd societix
```

Repo layout (top level):

```
societix/
├── build.gradle.kts            # root Gradle (Kotlin DSL)
├── settings.gradle.kts
├── gradlew / gradlew.bat       # Gradle wrapper — always use this
├── docker-compose.yml          # local infra (this guide)
├── .env.sample                 # copy to .env
├── app/                        # Spring Boot modular monolith (com.societix.*)
│   └── src/main/resources/db/migration/   # Flyway V__/R__ scripts
└── web/                        # React 18 + TS + Vite console
```

## 3. Local infrastructure (Docker Compose)

Create / use the repo's `docker-compose.yml`. It brings up **PostgreSQL 16 (with pgvector)**,
**Redis 7**, and a **Payment-Aggregator sandbox mock**. There is **no message broker** — the
event backbone is a Postgres transactional outbox (see
[ADR-0004](../adr/0004-kafka-event-driven-cadence.md)), so local setup is one container lighter.

```yaml
# docker-compose.yml
services:
  postgres:
    image: pgvector/pgvector:pg16          # PostgreSQL 16 + pgvector preinstalled
    container_name: soc-postgres
    environment:
      POSTGRES_DB: societix
      POSTGRES_USER: societix
      POSTGRES_PASSWORD: societix
    ports: ["5432:5432"]
    volumes: ["soc-pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U societix -d societix"]
      interval: 5s
      timeout: 3s
      retries: 20

  redis:
    image: redis:7-alpine
    container_name: soc-redis
    command: ["redis-server", "--appendonly", "yes"]
    ports: ["6379:6379"]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 20

  # NOTE: no Kafka/broker service in v1 — the event backbone is a Postgres outbox (ADR-0004).

  pa-sandbox:
    # Payment Aggregator sandbox/mock (Razorpay/Cashfree stand-in).
    # Serves fake VAN/VPA provisioning + fires payment.settled webhooks back to the app.
    image: mockserver/mockserver:5.15.0
    container_name: soc-pa-sandbox
    ports: ["1080:1080"]
    environment:
      MOCKSERVER_INITIALIZATION_JSON_PATH: /config/pa-expectations.json
    volumes:
      - ./infra/pa-mock:/config:ro

volumes:
  soc-pgdata:
```

Bring it up and wait for healthy:

```bash
docker compose up -d
docker compose ps          # all services should show (healthy)
```

> The `pa-sandbox` loads `infra/pa-mock/pa-expectations.json` (checked into the repo). It exposes
> `POST /pa/van` (provision a VAN), `POST /pa/vpa` (provision a UPI VPA), and a helper
> `POST /pa/simulate/payment` that calls your app's webhook `POST /api/v1/webhooks/payments` to
> drive zero-touch reconciliation locally — no real money, no real aggregator account.

## 4. Environment variables (`.env`)

Copy the sample and keep secrets out of git (`.env` is git-ignored):

```bash
cp .env.sample .env
```

```dotenv
# .env.sample  — local development defaults

# --- Database (PostgreSQL 16 + pgvector) ---
DB_URL=jdbc:postgresql://localhost:5432/societix
DB_USER=societix
DB_PASSWORD=societix

# --- Redis 7 ---
REDIS_HOST=localhost
REDIS_PORT=6379

# --- Event backbone ---
# No broker in v1. The transactional outbox lives in PostgreSQL; a scheduled poller
# (ShedLock-guarded) dispatches events. Tunables:
OUTBOX_POLL_INTERVAL_MS=1000
OUTBOX_BATCH_SIZE=100

# --- Payment Aggregator sandbox (Razorpay/Cashfree stand-in) ---
PA_PROVIDER=mock                         # mock | razorpay | cashfree
PA_BASE_URL=http://localhost:1080
PA_KEY_ID=rzp_test_LOCALSANDBOXKEY
PA_KEY_SECRET=local_sandbox_secret_do_not_commit
PA_WEBHOOK_SECRET=whsec_local_dev_only   # HMAC used to verify inbound webhooks

# --- LLM provider (AI Copilot RAG) — placeholder, leave blank to run in stub mode ---
LLM_PROVIDER=anthropic
LLM_MODEL=claude-sonnet-5-5              # default per canonical stack
LLM_API_KEY=                            # paste your key; blank => copilot returns stub answers

# --- App ---
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080
JWT_ISSUER=https://local.societix.dev
```

> **Never** commit a real `LLM_API_KEY`, `PA_KEY_SECRET`, or any production secret. CI and prod
> inject these from the secret manager (see [`../delivery/cicd-devops.md`](../delivery/cicd-devops.md)).

## 5. Database migrations (Flyway) + seed a demo tenant

Flyway runs the versioned `V__`/repeatable `R__` scripts under
`app/src/main/resources/db/migration` — including the RLS enable/force + `tenant_isolation`
policies and the `pgvector` extension (see [`../03-data-model.md`](../03-data-model.md) §3, §6).

```bash
# 1. Apply schema + RLS policies + pgvector
./gradlew flywayMigrate

# 2. Seed a demo society "Maple Residency" with chart-of-accounts, roles, and demo users.
#    The seed task sets SET LOCAL app.tenant_id for the demo tenant so RLS applies during seeding.
./gradlew seedDemo
```

`seedDemo` creates:

| What | Value |
|---|---|
| Society (tenant) | `Maple Residency` — `tenant_id = soc_maple_01`, state `MH` |
| Units | `A-101 … A-110`, `B-1201 … B-1210` |
| Secretary login | `secretary@maple.demo` / `Password123!` |
| Resident login | `resident.a101@maple.demo` / `Password123!` |
| Treasurer login | `treasurer@maple.demo` / `Password123!` |
| Sample invoices | one `RAISED` maintenance invoice per unit, current month |

> If a migration fails mid-way, see the troubleshooting table (§10). To start completely clean:
> `docker compose down -v && docker compose up -d && ./gradlew flywayMigrate seedDemo`.

## 6. Run the backend

```bash
./gradlew bootRun
```

The app reads `.env` (via the Spring config import) and starts on `http://localhost:8080`.
Verify:

```bash
curl -s http://localhost:8080/actuator/health | jq .     # {"status":"UP"}
open http://localhost:8080/swagger-ui.html               # OpenAPI 3.1 — the API source of truth
```

API conventions: base path `/api/v1`, problem+json errors, idempotency keys — see
[`api-guidelines.md`](api-guidelines.md).

## 7. Run the web console

```bash
cd web
npm install
cp .env.sample .env.local        # sets VITE_API_BASE_URL=http://localhost:8080/api/v1
npm run dev
```

Vite serves the console at `http://localhost:5173` and proxies API calls to the backend.
If the API runs elsewhere, edit `VITE_API_BASE_URL` in `web/.env.local`.

## 8. Log in as a seeded user

1. Open `http://localhost:5173`.
2. **Secretary (admin view):** `secretary@maple.demo` / `Password123!` — full MC console
   (treasury, governance, gate, privacy).
3. **Resident (member view):** `resident.a101@maple.demo` / `Password123!` — their unit's
   invoices, AGM ballots, gate passes.

To exercise zero-touch reconciliation end-to-end, trigger a sandbox settlement:

```bash
curl -s -X POST http://localhost:1080/pa/simulate/payment \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"soc_maple_01","unit":"A-101","amountMinor":250000}'
# => fires payment.settled → matcher allocates → invoice flips to PAID in the UI
```

## 9. Run the tests

```bash
./gradlew test             # unit + slice tests
./gradlew integrationTest  # Testcontainers-backed (Postgres, Redis)
./gradlew check            # tests + Spotless + static analysis (Error Prone / SpotBugs)
```

> **Testcontainers note:** `integrationTest` spins up **real** Postgres 16 and Redis containers
> automatically — it does **not** use the `docker-compose` stack, so it is safe to
> run while `docker compose up` is live. You only need Docker running; no manual container setup.
> On CI the Docker socket is provided by the runner. First run pulls images (slow); later runs are
> cached. Tenancy/RLS isolation tests live under `app/src/integrationTest` and assert that a query
> without `app.tenant_id` fails closed.

Frontend tests:

```bash
cd web && npm test         # Vitest + React Testing Library
```

## 10. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `bootRun` fails: `Connection refused :5432` | Postgres not up/healthy | `docker compose ps`; wait for `(healthy)`; `docker compose up -d postgres` |
| `FATAL: database "societix" does not exist` | Volume created before env set | `docker compose down -v && docker compose up -d` |
| Flyway: `relation already exists` / checksum mismatch | Edited an applied `V__` script | Never edit applied migrations — add a new `V__`; or reset DB (`down -v`) locally |
| Query returns 0 rows but data exists | `app.tenant_id` GUC not set (RLS fail-closed) | Ensure you hit the API via a JWT; don't query the DB directly without `SET LOCAL app.tenant_id` |
| Outbox events never dispatched (e.g. no reminder sent) | Poller not running / ShedLock held by a dead instance | Check app logs for the outbox poller; verify the `shedlock` row isn't stuck locked; confirm `OUTBOX_POLL_INTERVAL_MS` is set |
| Copilot returns `"(stub answer)"` | `LLM_API_KEY` blank | Paste a key in `.env`; restart `bootRun` |
| Payment webhook 401 | `PA_WEBHOOK_SECRET` mismatch | Match `.env` to the value in `infra/pa-mock/pa-expectations.json` |
| Web console: CORS / 404 on `/api/v1` | Wrong `VITE_API_BASE_URL` | Set it to `http://localhost:8080/api/v1` in `web/.env.local` |
| `./gradlew` wrong Java | `JAVA_HOME` points elsewhere | `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` |
| Testcontainers: `Could not find a valid Docker environment` | Docker not running | Start Docker Desktop; `docker info` must succeed |

---

**Related:** [`coding-standards.md`](coding-standards.md) · [`api-guidelines.md`](api-guidelines.md) ·
[`git-workflow.md`](git-workflow.md) · [`../01-architecture.md`](../01-architecture.md) ·
[`../delivery/cicd-devops.md`](../delivery/cicd-devops.md)
