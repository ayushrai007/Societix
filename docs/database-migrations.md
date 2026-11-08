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

The backend connects to `jdbc:postgresql://localhost:5432/societix` as user `societix` by default.
The password is never committed — set it in your environment before starting the app:

```bash
export SPRING_DATASOURCE_PASSWORD=<your local password>
cd backend
./mvnw spring-boot:run
```

To point at a different database, also set `SPRING_DATASOURCE_URL` and
`SPRING_DATASOURCE_USERNAME`.
