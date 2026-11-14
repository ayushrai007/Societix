# Getting Started — Local Development

**Status:** Standard (prescriptive)

This guide takes a new engineer from an empty machine to a **running Societix stack** —
backend, local infra, and web console. Follow it top to bottom.
Architecture context: [`../archive/01-architecture.md`](../archive/01-architecture.md).
Data model & RLS: [`../archive/03-data-model.md`](../archive/03-data-model.md). Terms: [`../06-glossary.md`](../06-glossary.md).

> **TL;DR (current baseline)** — `cd backend && ./mvnw spring-boot:run` → check `http://localhost:8080/health` or `http://localhost:8080/actuator/health` → run tests with `cd backend && ./mvnw test` / `./mvnw verify`.
> Local Docker Compose infrastructure, `.env.example`, payment sandbox, and web console are delivered in upcoming backlog tasks (SOC-3 and front-end milestones).

---

## 1. Prerequisites

Install these **exact** major versions. Others may work but CI pins these.

| Tool | Version | Install (macOS) | Verify |
|---|---|---|---|
| JDK | **Temurin 25 (LTS)** | `brew install --cask temurin@25` | `java -version` → `25.x` |
| Maven | **use the wrapper** (`backend/mvnw`, Maven 3.10.0 / Wrapper 3.3.4) | committed in repo — do **not** install Maven globally | `cd backend && ./mvnw --version` |
| Docker Desktop | latest; ≥ 6 GB RAM allocated | `brew install --cask docker` | `docker info` |
| Node.js | **26 LTS** | `brew install node@26` | `node -v` → `v26.x` |
| Git | ≥ 2.40 | `brew install git` | `git --version` |

> Set `JAVA_HOME` to the Temurin 25 path, e.g.
> `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` (add to your shell profile).
> On Linux/Windows install the equivalent Temurin 25 JDK and Docker Engine; the Maven Wrapper and
> `docker compose` commands are identical.

## 2. Clone

```bash
git clone git@github.com:ayushrai007/Societix.git
cd Societix
```

Repo layout (top level):

```
societix/
├── backend/
│   ├── pom.xml                 # Maven build configuration
│   ├── .mvn/wrapper/           # pinned Maven Wrapper configuration
│   ├── mvnw / mvnw.cmd         # Maven Wrapper — always use this
│   └── src/                    # Spring Boot modular monolith (com.societix.*)
├── docs/                       # engineering guides & ADRs
├── docker-compose.yml          # [arrives in SOC-3] local infra (Postgres 18, Redis 8.2, PA mock)
├── .env.example                # [arrives in SOC-3] environment template (git-unignored)
└── web/                        # [arrives in web front-end tasks] React 18 + TS console
```

## 3. Local infrastructure (Docker Compose)

`docker-compose.yml` brings up **PostgreSQL 18 with pgvector 0.8.7**, **Redis 8.2 (Extended
Support)**, and a **Payment-Aggregator sandbox mock**. The Compose file is the authoritative service
configuration. There is **no message broker** — the event backbone is a Postgres transactional
outbox (see [ADR-0008](../adr/0008-postgres-outbox-no-broker.md)).

Start the local services with:

```bash
docker compose up -d --wait
docker compose ps          # all services should show (healthy)
```

## 4. Environment variables (`.env.example`)

Copy the tracked local defaults from `.env.example`; `.env` and `.env.*` remain ignored:

```bash
cp .env.example .env
```

The backend imports the repository-root `.env` when it starts from `backend/`. See
[`../database-migrations.md`](../database-migrations.md) for the owner/runtime database roles.

> Spring does not read `.env` by itself. Export these variables into the shell that runs the
> backend (for example `set -a; source .env; set +a`); `SPRING_DATASOURCE_PASSWORD` has no default
> and startup fails without it. See [`../database-migrations.md`](../database-migrations.md).

> **Never** commit a real `LLM_API_KEY`, `PA_KEY_SECRET`, or any production secret. CI and prod
> inject these from the secret manager (see [`../archive/delivery/cicd-devops.md`](../archive/delivery/cicd-devops.md)).

## 5. Database migrations and demo data

Flyway applies the versioned SQL migrations in `backend/src/main/resources/db/migration`
automatically when `./mvnw spring-boot:run` starts; there is no separate migration command. See
[`../database-migrations.md`](../database-migrations.md) for how to add a migration.

The current scaffold does not configure demo seed data; there is no seed profile to run.

## 6. Run the backend

```bash
set -a; source .env; set +a
cd backend
./mvnw spring-boot:run
```

With the `.env` variables exported (see §4), the Spring Boot modular monolith starts on
`http://localhost:8080`.
Verify:

```bash
curl -s http://localhost:8080/health | jq .            # {"status":"UP"}
curl -s http://localhost:8080/actuator/health | jq .   # {"status":"UP"}
```

API conventions: base path `/api/v1`, RFC 9457 problem+json errors, idempotency keys — see
[`../archive/standards/api-guidelines.md`](../archive/standards/api-guidelines.md).
*(Note: Swagger / OpenAPI UI at `/swagger-ui.html` is planned for upcoming API documentation tasks and is not yet mounted).*

## 7. Run the web console (Planned — Front-end tasks)

The web console under `web/` will be delivered in the front-end milestones:

```bash
cd web
npm install
cp .env.example .env.local        # sets VITE_API_BASE_URL=http://localhost:8080/api/v1
npm run dev
```

Vite will serve the console at `http://localhost:5173` and proxy API calls to the backend.

## 8. Log in as a seeded user (Planned — SOC-11 / Demo Seed)

Once Auth (SOC-11) and demo seed profiles are implemented in upcoming tasks, test accounts will be available:
- **Secretary (admin view):** `secretary@maple.demo` / `Password123!`
- **Resident (member view):** `resident.a101@maple.demo` / `Password123!`

Zero-touch reconciliation sandbox trigger (arrives with SOC-103):

```bash
curl -s -X POST http://localhost:1080/pa/simulate/payment \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"soc_maple_01","unit":"A-101","amountMinor":250000}'
```

## 9. Run the tests

Run backend unit and module-verification tests:

```bash
cd backend
./mvnw test             # run configured tests
./mvnw verify           # run tests and package the backend jar
```

Testcontainers-backed integration tests will run automatically as part of `./mvnw verify` once
test suites are added.

## 10. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `spring-boot:run` fails: `Connection refused :5432` | Postgres not up/healthy | Start Postgres via Docker once SOC-3 lands; verify with `docker compose ps` |
| `FATAL: database "societix" does not exist` | Volume created before env set | `docker compose down -v && docker compose up -d` |
| Flyway: `relation already exists` / checksum mismatch | Edited an applied `V__` script | Never edit applied migrations — add a new `V__`; or reset DB (`down -v`) locally |
| Query returns 0 rows but data exists | `app.tenant_id` GUC not set (RLS fail-closed) | Ensure you hit the API via a JWT; don't query the DB directly without `SET LOCAL app.tenant_id` |
| Outbox events never dispatched | Poller not running / ShedLock held | Check app logs for the outbox poller once SOC-20/21 land; confirm `OUTBOX_POLL_INTERVAL_MS` is set |
| Payment webhook 401 | `PA_WEBHOOK_SECRET` mismatch | Match `.env` to the value in `infra/pa-mock/pa-expectations.json` |
| `backend/mvnw` uses the wrong Java | `JAVA_HOME` points elsewhere | `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` |
| Testcontainers: `Could not find a valid Docker environment` | Docker not running | Start Docker Desktop; `docker info` must succeed |

---

**Related:** [`coding-standards.md`](coding-standards.md) · [`../archive/standards/api-guidelines.md`](../archive/standards/api-guidelines.md) ·
[`git-workflow.md`](git-workflow.md) · [`../archive/01-architecture.md`](../archive/01-architecture.md) ·
[`../archive/delivery/cicd-devops.md`](../archive/delivery/cicd-devops.md)
