# SOC-5 — Flyway baseline

Pull request: [#9](https://github.com/ayushrai007/Societix/pull/9)  
Merge state: **Open, not merged**. Branch `origin/fm/soc-5-flyway`, head `2f20941`.

## What it delivers and why

SOC-5 introduces Flyway-managed database history with an empty V1 baseline. Flyway runs on backend startup, records applied migrations, and validates them so future schema changes are repeatable and tampering is detected.

## Files and modules touched

- `backend/pom.xml` adds the Spring Boot Flyway starter, PostgreSQL Flyway support, and JDBC driver.
- `backend/src/main/resources/application.yml` configures the local datasource and `classpath:db/migration`.
- `backend/src/main/resources/db/migration/V1__baseline.sql` is the intentionally empty starting migration.
- `backend/src/test/java/com/societix/FlywayMigrationTest.java` verifies startup migration, the history table, V1 metadata, no pending migrations, and checksum validation.
- `backend/src/test/java/com/societix/TestcontainersConfiguration.java` and existing application tests use a real `pgvector/pgvector:0.8.7-pg18` Testcontainers database.
- `docs/database-migrations.md`, `docs/standards/getting-started.md`, `docs/README.md`, and `docs/codegen/02-tech-blueprint.md` document the workflow and dependency.

## Local run and verification

Docker must be running. Against the Compose database:

```bash
cp .env.example .env
docker compose up -d --wait
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/societix
export SPRING_DATASOURCE_USERNAME=societix
export SPRING_DATASOURCE_PASSWORD=societix
(cd backend && ./mvnw -B test -Dtest=FlywayMigrationTest)
(cd backend && ./mvnw spring-boot:run)
curl -s http://localhost:8080/health
(cd backend && ./mvnw -B clean verify)
docker compose down
```

On first startup the database gets one `V1__baseline.sql` row in `flyway_schema_history`; a restart skips it. The integration tests use Testcontainers and need Docker independently of Compose.

## Configuration and environment

The application uses standard Spring names: `SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_USERNAME` override the local URL/user, while `SPRING_DATASOURCE_PASSWORD` has no committed default and must be supplied. Flyway locations are `classpath:db/migration`. The current branch is intentionally not using the SOC-3 `DB_*` names.

## Key decisions

- Use an empty V1 baseline to establish migration history before real tables exist.
- Apply migrations automatically at startup and test against real PostgreSQL 18/pgvector.
- Never edit or delete an applied migration; add the next version (`V2__...`) instead. This preserves Flyway checksums and supports repeatable deployments.
- Keep RLS enablement and policies in the same migration as each future tenant table, as repository conventions require.

## Known limits and follow-ups

The baseline contains no domain tables. `FlywayMigrationTest` currently expects exactly one applied migration and must be relaxed when V2 lands. Because datasource startup now requires PostgreSQL and a password, the unmerged SOC-3 local stack (or another PostgreSQL) is needed for a fresh developer run.

## Evidence

This note was checked against the exact `origin/fm/soc-5-flyway` files and PR state. The branch's recorded verification includes startup, restart, checksum-tampering, missing-password, and Testcontainers scenarios; the current merge state remains open.

