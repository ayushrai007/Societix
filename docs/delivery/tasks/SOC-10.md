# SOC-10 — Society tenant entity and admin onboarding

## Delivery and why

SOC-10 establishes the tenant root for an apartment complex and the first onboarding path. It
delivers a persisted Society record with its legal-rule state, a platform-admin create endpoint,
database migration, and the database isolation backstop needed before later society-owned data is
added.

## Touched files and modules

- `backend/pom.xml` and `backend/src/main/resources/application.yml`: enable Spring Data JPA and
  schema validation.
- `backend/src/main/resources/db/migration/V2__create_society.sql`: create `society` with `id`,
  `tenant_id`, `name`, `address`, and `state`; enable and force fail-closed RLS.
- `backend/src/main/java/com/societix/platform/society/`: platform Society entity, repository,
  onboarding service, request/response DTOs, and controller.
- `backend/src/main/java/com/societix/platform/error/`: RFC 9457 problem response and global
  validation/authentication/authorization handling.
- `backend/src/test/java/com/societix/FlywayMigrationTest.java`: migration and policy assertions.
- `backend/src/test/java/com/societix/SocietyOnboardingIT.java`: PostgreSQL Testcontainers API,
  validation, persistence, and RLS isolation tests.
- `docs/api/societies.md`: public endpoint behavior.
- `docs/jira/backlog.md` and `docs/jira/societix-backlog.csv`: assign SOC-10 to Aditya.

## Verification

From `backend/`, with a Java 25 JDK on `JAVA_HOME` and a running Docker daemon:

```bash
./mvnw test
```

The suite is intended to start `pgvector/pgvector:0.8.7-pg18` through Testcontainers. The focused
SOC-10 suite is:

```bash
./mvnw -Dtest=SocietyOnboardingIT test
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
5. `SocietyOnboardingIT.invalidSocietyRequestReturnsProblemJson` proves validation errors use
   `application/problem+json`.

Verification status at the time of writing: the branch compiles (`./mvnw test-compile`) and
`ModularityTest` passes on Java 25. Every other suite, including `SocietyOnboardingIT` and
`FlywayMigrationTest`, needs Testcontainers, and the Docker daemon on the authoring machine was
down, so those suites have not yet been run green locally. CI (`./mvnw -B verify` on
pull requests to `master`) runs them with Docker available.

## Configuration and environment

- Runtime database configuration uses `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
  `SPRING_DATASOURCE_PASSWORD`; no credentials are committed.
- Local tests require Docker and the Testcontainers image above.
- `./mvnw` uses the Java 25 JDK found on `JAVA_HOME` (or `PATH`).

## Decisions and reasons

- `tenant_id` equals `id` for the Society tenant registry. This keeps the root row compatible with
  the shared-schema tenancy convention while allowing onboarding to set the new tenant context in
  its transaction.
- RLS is enabled and forced with a regex-guarded UUID context, so absent or malformed
  `app.tenant_id` exposes no rows and does not raise a cast error.
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
- The PR must merge after SOC-5 PR #9; this task does not merge either PR.

## Pull request

PR link: https://github.com/ayushrai007/Societix/pull/13
