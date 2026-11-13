# SOC-10 — Society tenant entity and admin onboarding

## Delivery and why

SOC-10 establishes the tenant root for an apartment complex and the first onboarding path. It
delivers a persisted Society record with its legal-rule state, a platform-admin create endpoint,
database migration, and database-level tenant isolation (row-level security) that applies to the
running application, not only to a test role, before later society-owned data is added.

The application connects to PostgreSQL as a dedicated runtime role that is not a superuser, cannot
bypass RLS and owns no tables. Superusers and table owners skip RLS policies, so without this role
the policies would exist but never apply to the application's own queries.

## Touched files and modules

- `backend/pom.xml`: enable Spring Data JPA, and run the `*IT.java` integration tests during
  `./mvnw verify` (Maven Failsafe; previously they were never run).
- `backend/src/main/resources/application.yml`: schema validation; the application datasource
  uses the runtime role and Flyway uses the owner role.
- `backend/src/main/resources/db/migration/V2__create_society.sql`: create `society` with `id`,
  `tenant_id`, `name`, `address`, and `state`; enable and force fail-closed RLS.
- `backend/src/main/resources/db/migration/V3__grant_app_runtime_role.sql`: grant the runtime role
  `SELECT, INSERT` on `society` (and create it `NOLOGIN` if no environment provisioned it).
- `infra/postgres/initdb/10-create-app-runtime-role.sh`: creates the runtime login role on first
  database start; shared by `docker-compose.yml` and the Testcontainers PostgreSQL container.
- `docker-compose.yml` and `.env.example`: owner and runtime role credentials; the init script
  mount; a TCP healthcheck so the database is only healthy after init scripts finish.
- `backend/src/main/java/com/societix/platform/society/`: platform Society entity, repository,
  onboarding service, request/response DTOs, and controller.
- `backend/src/main/java/com/societix/platform/error/`: RFC 9457 problem response and global
  validation/authentication/authorization handling.
- `backend/src/test/java/com/societix/TestcontainersConfiguration.java`: the test database has
  the same two roles; the application context connects as the runtime role.
- `backend/src/test/java/com/societix/FlywayMigrationTest.java`: migration and policy assertions,
  read through Flyway's owner connection.
- `backend/src/test/java/com/societix/SocietyOnboardingIT.java`: PostgreSQL Testcontainers API,
  validation, persistence, and RLS isolation tests, all on the application's own connection.
- `backend/src/test/java/com/societix/LocalDevEnvironmentTest.java`: the compose stack connects the
  application as the runtime role.
- `docs/database-migrations.md`, `docs/01-start-here.md`, `docs/standards/getting-started.md`: the
  two roles and their settings.
- `docs/api/societies.md`: public endpoint behavior.
- `docs/jira/backlog.md` and `docs/jira/societix-backlog.csv`: assign SOC-10 to Aditya.

## Verification

From `backend/`, with a Java 25 JDK on `JAVA_HOME` and a running Docker daemon:

```bash
./mvnw verify
```

This runs the unit tests and the `*IT.java` integration tests, which start
`pgvector/pgvector:0.8.7-pg18` through Testcontainers. The focused SOC-10 suite is:

```bash
./mvnw verify -Dtest=FlywayMigrationTest -Dit.test=SocietyOnboardingIT
```

Acceptance mapping:

1. `SocietyOnboardingIT.adminCanCreateSocietyViaEndpoint` proves `201 Created` and `Location`.
2. `SocietyOnboardingIT.createdSocietyContainsIdNameAddressAndState` proves the response and
   persisted row contain the required fields.
3. `FlywayMigrationTest.societyMigrationAppliesCleanly` proves V2 created the table, columns, and
   RLS policy.
4. `SocietyOnboardingIT.societyRowsAreIsolatedByTenantRls` proves tenant A cannot read tenant B,
   and missing/invalid tenant context is fail-closed;
   `SocietyOnboardingIT.societyCannotWriteAnotherTenantsRow` proves tenant A cannot insert a row
   owned by tenant B and that the rejected row is not persisted.
   `SocietyOnboardingIT.untenantedQueryFailsClosedForTheApplicationRuntimeRole` proves the
   application's own datasource is the restricted runtime role (not superuser, no BYPASSRLS, not
   the table owner) and that its queries without tenant context, both SQL and
   `SocietyRepository`, return no rows while the rows exist for their own tenants.
   None of these tests switches database role.
5. `SocietyOnboardingIT.invalidSocietyRequestReturnsProblemJson` proves validation errors use
   `application/problem+json`.

Verification status at the time of writing: `./mvnw -B verify` passes locally on Java 25 with
Docker: 12 unit/Testcontainers tests (including `FlywayMigrationTest` and
`LocalDevEnvironmentTest`) and the 6 `SocietyOnboardingIT` tests. As a check that the isolation
tests can fail, pointing the application datasource back at the superuser makes
`untenantedQueryFailsClosedForTheApplicationRuntimeRole`, `societyRowsAreIsolatedByTenantRls` and
`societyCannotWriteAnotherTenantsRow` fail. CI runs `./mvnw -B verify` on pull requests to
`master`.

## Configuration and environment

- The application connects as the runtime role: `SPRING_DATASOURCE_URL`,
  `SPRING_DATASOURCE_USERNAME` (default `societix_app`) and `SPRING_DATASOURCE_PASSWORD`.
- Flyway connects to the same database as the owner role: `SPRING_FLYWAY_USER` (default
  `societix`) and `SPRING_FLYWAY_PASSWORD`.
- Docker Compose creates both roles from those variables. Init scripts run only on an empty data
  volume, so an existing local database needs `docker compose down -v` once.
- No credentials are committed beyond the throwaway local defaults in `.env.example`.
- Local tests require Docker and the Testcontainers image above.
- `./mvnw` uses the Java 25 JDK found on `JAVA_HOME` (or `PATH`).

## Decisions and reasons

- `tenant_id` equals `id` for the Society tenant registry. This keeps the root row compatible with
  the shared-schema tenancy convention while allowing onboarding to set the new tenant context in
  its transaction.
- RLS is enabled and forced with a regex-guarded UUID context, so absent or malformed
  `app.tenant_id` exposes no rows and does not raise a cast error.
- The application uses a separate runtime role rather than the owner role. PostgreSQL skips RLS
  for superusers and table owners even with `FORCE ROW LEVEL SECURITY`, so this is what makes the
  policies apply to the application. The earlier test switched to a `NOBYPASSRLS` role inside
  the test, which proved the policy but not the application's isolation; the tests now use the
  application's own connection.
- Until SOC-11/SOC-12 signed JWT and RBAC work lands, the endpoint requires
  `X-Admin-Role: PLATFORM_ADMIN`. This is a caller-controlled temporary gate, not a production
  authentication mechanism; it is explicitly an open security item.
- SOC-13 request-level JWT-derived isolation is intentionally excluded: it is a separate backlog
  task with SOC-11 as a precondition.

## Limits and follow-ups

- Replace the temporary role header with server-side authentication/authorization. The proposed
  interim fix is a server-side admin key supplied through configuration until SOC-12 roles land;
  this requires Ayush's agreement.
- SOC-13 will add the request boundary that derives tenant context from a verified JWT and expands
  request-level isolation coverage.
- Production and CI-deployed environments must provision the runtime login role (and its
  password) the same way the init script does; the V3 fallback role is `NOLOGIN`.
- Each future tenant table's migration must grant the runtime role its privileges. A default-
  privileges or startup check that refuses to run as a superuser/BYPASSRLS role is a possible
  follow-up and is not part of SOC-10.
- The PR must merge after SOC-5 PR #9; this task does not merge either PR.

## Pull request

PR link: https://github.com/ayushrai007/Societix/pull/13
