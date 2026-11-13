# Database Migrations (Flyway)

**What this is:** how the backend's database schema is created and changed.
**Who should read it:** anyone adding or changing a table, column, index, or policy.

We never change the database by hand. Every schema change is a small SQL file in the backend,
and [Flyway](https://documentation.red-gate.com/fd/) applies those files, in order, every time the
application starts. That way every laptop, CI run, and server ends up with the identical schema.

## Where migrations live

```
backend/src/main/resources/db/migration/
└── V1__baseline.sql      # empty baseline: the start of the schema history
```

When the backend starts, Flyway compares the files in that folder with the
`flyway_schema_history` table in the database and runs any file it has not run yet. Files that were
already applied are skipped, so starting the app again is safe.

## Naming convention

```
V<version>__<description>.sql
```

- `V` — capital V, for a *versioned* migration.
- `<version>` — the next whole number: `V2`, `V3`, `V4`, … Versions run in numeric order and must
  never repeat.
- `__` — **two** underscores separate the version from the description.
- `<description>` — lowercase words joined by single underscores, saying what the change does.

Examples: `V2__create_society_table.sql`, `V3__add_unit_number_index.sql`.

## How to add a new migration

1. Look in `backend/src/main/resources/db/migration/` and find the highest version number.
2. Create a new file with the next number, e.g. `V2__create_society_table.sql`.
3. Write plain PostgreSQL SQL in it. For any new tenant table, put the
   `ENABLE ROW LEVEL SECURITY` and `CREATE POLICY` statements in the **same** migration (see
   [codegen conventions](codegen/01-conventions.md)).
4. Run the backend tests from `backend/`:

   ```bash
   ./mvnw -B clean verify
   ```

   The tests start a real PostgreSQL 18 database in Docker (Testcontainers), so Docker must be
   running. `FlywayMigrationTest` fails if any migration does not apply cleanly.
5. Commit the migration together with the code that uses it.

## Rules

- **Never edit or rename a migration after it has been merged.** Flyway stores a checksum of every
  applied file and refuses to start if one changes. To fix a mistake, add a new migration.
- **Never delete a migration** that has been applied anywhere.
- One logical change per migration; keep them small.
- No personal data in migrations (no seed rows with real names, phones, or emails).

## Running the backend against a local database

The backend uses two database roles on `jdbc:postgresql://localhost:5432/societix`:

| Role (default name) | Used by | Settings |
|---|---|---|
| `societix` (owner) | Flyway, to run migrations; owns the schema and tables | `SPRING_FLYWAY_USER`, `SPRING_FLYWAY_PASSWORD` |
| `societix_app` (runtime) | The application's own queries | `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |

The runtime role is not a superuser, cannot bypass row-level security and owns no tables, so the
tenant RLS policies apply to every query the application makes; a query without a valid
`app.tenant_id` sees no tenant rows. Superusers and table owners bypass RLS, which is why the
application must never connect as the owner role.

Locally, `docker compose up` creates both roles: the owner is the image's `POSTGRES_USER`, and
`infra/postgres/initdb/10-create-app-runtime-role.sh` creates the runtime role. Init scripts run
only on an empty data volume, so a database created before the runtime role existed needs
`docker compose down -v` once. Migrations grant the runtime role the table privileges it needs
(for example `V3__grant_app_runtime_role.sql`); a migration that adds a tenant table must grant
it too.

Passwords are never committed. The backend reads the repository-root `.env` (copied from
`.env.example`) when started from `backend/`; otherwise set the four variables in your
environment before starting the app:

```bash
cd backend
./mvnw spring-boot:run
```

To point at a different database, also set `SPRING_DATASOURCE_URL`.
