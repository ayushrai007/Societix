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

SOC-11 stores verified Indian phone contacts encrypted with AES-256-GCM. Generate a local key and
put it in the ignored `.env` file as `SOCIETIX_PHONE_ENCRYPTION_KEY`:

```bash
openssl rand -base64 32
```

Do not commit or share this key. Preserve it for the lifetime of local data; replacing it requires
re-encrypting stored phone contacts. Production should supply the key through a managed secret or
key-management service. `FIREBASE_ADMIN_ENABLED` defaults to `false`, so the backend can start
without Firebase Admin credentials. Applications can be submitted, but approvals, role changes, and
suspension return `503` until Firebase Admin is configured, preventing a database change that cannot
be propagated to ID-token claims. To enable claims sync, set it to `true` and provide Application
Default Credentials or workload identity. See the [platform-admin bootstrap
runbook](../security/platform-admin-bootstrap.md) for the one-time operator setup.

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
curl -s http://localhost:8080/actuator/metrics | jq .   # available metric names
curl -s 'http://localhost:8080/actuator/metrics/http.server.requests?tag=status:400' | jq .
```

Actuator metrics use the same application port and are intended for local diagnostics. They expose
request and JVM measurements; keep this endpoint behind trusted network access when deploying the
service.

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

## 8. Test Firebase authentication and onboarding

SOC-11 adds Firebase ID-token authentication and membership onboarding endpoints. For local testing,
use the Firebase Authentication Emulator so no live Firebase project or SMS is needed:

1. Install Node.js and the Firebase CLI (`npm install -g firebase-tools`). From the repository root,
   start Auth Emulator with the demo project ID:

   ```bash
   firebase emulators:start --only auth --project demo-societix-local
   ```

   The Auth Emulator UI is at `http://127.0.0.1:4000`.
2. In a separate terminal, enable emulator mode and Firebase Admin integration in the ignored
   `.env` file:

   ```dotenv
   FIREBASE_PROJECT_ID=demo-societix-local
   FIREBASE_ADMIN_ENABLED=false
   FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099
   SOCIETIX_SOCIETY_CREATION_ENABLED=true
   SOCIETIX_PLATFORM_ADMIN_BOOTSTRAP_FIREBASE_UID=
   ```

   Keep `FIREBASE_ADMIN_ENABLED=false`; setting `FIREBASE_AUTH_EMULATOR_HOST` enables the Admin SDK
   against the emulator without production credentials. The `demo-` project ID is required so
   unsigned emulator tokens cannot be enabled against a real Firebase project. Society creation
   remains disabled by default and the setting above enables it only for local testing. The emulator setting must be an actual
   process environment variable because the Firebase Admin SDK reads it directly. From `backend/`,
   start the server with:

   ```bash
   set -a; source ../.env; set +a
   ./mvnw spring-boot:run
   ```
3. Use the Auth Emulator UI to create a phone-auth user. Sign in and print that user's ID token with
   the repository helper (it reads the SMS code from the emulator REST interface, so no client SDK
   is needed):

   ```bash
   cd ..
   ./scripts/firebase-emulator-signin.py
   ```

   Enter the user's phone number in E.164 format. The script prints only the ID token to stdout and
   sends progress/errors to stderr. Capture it with `ADMIN_ID_TOKEN="$(../scripts/firebase-emulator-signin.py)"`
   when running from `backend/`. Create one user for the platform admin and a separate phone-auth
   user to exercise membership requests. Emulator phone sign-in generates local verification codes;
   it does not send a real SMS.
4. To test the approval flow, set `SOCIETIX_PLATFORM_ADMIN_BOOTSTRAP_FIREBASE_UID` to the emulator
   user's UID for one backend startup. The backend creates the local platform-admin record and sets
   its custom claims in the emulator. Clear the UID setting afterwards. This bootstrap modifies only
   the local Societix database and emulator.
5. Submit a membership request with the second user's ID token and a verified Indian test number.
   Approvals sync claims through the Auth Emulator. Local phone contact encryption still requires a
   local key; generate one with `openssl rand -base64 32` and set
   `SOCIETIX_PHONE_ENCRYPTION_KEY` in `.env`.

Emulator tokens are unsigned and must never be accepted by a deployed environment. Keep
`FIREBASE_AUTH_EMULATOR_HOST` empty outside local development. Startup fails if emulator mode is
configured with a project ID that does not start with `demo-`, and startup logs a warning when the
emulator verifier is enabled. Without the setting, the backend verifies Firebase's production token
signature.

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
| Outbox events never dispatched | Poller not running / ShedLock held | Check app logs for the outbox poller once SOC-20/21 land; confirm `SOCIETIX_OUTBOX_POLL_DELAY_MS` is set (default: 3000 ms) |
| Payment webhook 401 | `PA_WEBHOOK_SECRET` mismatch | Match `.env` to the value in `infra/pa-mock/pa-expectations.json` |
| `backend/mvnw` uses the wrong Java | `JAVA_HOME` points elsewhere | `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` |
| Testcontainers: `Could not find a valid Docker environment` | Docker not running | Start Docker Desktop; `docker info` must succeed |

---

**Related:** [`coding-standards.md`](coding-standards.md) · [`../archive/standards/api-guidelines.md`](../archive/standards/api-guidelines.md) ·
[`git-workflow.md`](git-workflow.md) · [`../archive/01-architecture.md`](../archive/01-architecture.md) ·
[`../archive/delivery/cicd-devops.md`](../archive/delivery/cicd-devops.md)
