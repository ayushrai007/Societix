# SOC-3 — Local development environment

Pull request: [#7](https://github.com/ayushrai007/Societix/pull/7)  
Merge state: **Open, not merged**. Branch `origin/fm/soc-3-local-dev`, head `b85ab2d`.

## What it delivers and why

SOC-3 supplies a reproducible local stack: PostgreSQL 18 with pgvector, Redis 8.2, and a local MockServer payment-aggregator sandbox. It starts dependencies and the backend without real payment traffic; events still use the planned PostgreSQL outbox, so no broker is added.

## Files and modules touched

- `docker-compose.yml` defines the three services, localhost-only ports, health checks, and the PostgreSQL 18 volume path.
- `.env.example` defines local `DB_*`, `REDIS_PORT`, and `PA_MOCK_PORT` values.
- `infra/pa-mock/pa-expectations.json` supplies the mock's `GET /pa/status` response.
- `backend/pom.xml`, `backend/src/main/resources/application.yml`, and `backend/src/test/java/com/societix/TestcontainersConfiguration.java` add JDBC/Testcontainers wiring.
- `backend/src/test/java/com/societix/LocalDevEnvironmentTest.java` starts the repository Compose file on ephemeral ports and checks images, health, mock response, pgvector, and database health.
- `docs/01-start-here.md` and `docs/codegen/02-tech-blueprint.md` update local-start guidance and the MockServer version.

## Local run and verification

```bash
cp .env.example .env
docker compose up -d --wait
docker compose ps
curl -s http://localhost:1080/pa/status
(cd backend && ./mvnw -B verify)
(cd backend && ./mvnw spring-boot:run)
curl -s http://localhost:8080/actuator/health
docker compose down
```

`LocalDevEnvironmentTest` requires Docker and the `docker compose` CLI. Its ComposeContainer overrides ports to zero, so it can run beside a developer's own stack.

## Configuration and environment

Compose reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `REDIS_PORT`, and `PA_MOCK_PORT` from `.env` with local defaults. The backend imports the optional repository-root `.env` and binds the `DB_*` datasource names. The payment mock disables dashboard analytics; no real credentials or money are used.

## Key decisions

- Pin `pgvector/pgvector:0.8.7-pg18`, `redis:8.2-alpine`, and `mockserver/mockserver:9.2.0`.
- Bind ports to `127.0.0.1` and disable MockServer dashboard analytics for local privacy.
- Mount PostgreSQL 18 data at `/var/lib/postgresql`, and use ephemeral ports in the integration test to avoid collisions.
- Keep the payment provider as a canned sandbox and omit a broker because the architecture uses the PostgreSQL outbox.

## Known limits and follow-ups

The backend does not yet connect to Redis. The older `docs/standards/getting-started.md` snippet still needs alignment with this Compose setup, and CI runners need Docker Compose for the ComposeContainer test.

## Evidence

This note is based on the exact `origin/fm/soc-3-local-dev` head and its changed files, not issue prose.

