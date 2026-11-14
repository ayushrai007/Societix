#!/usr/bin/env bash
# Creates the login role the Societix application uses at runtime.
#
# The PostgreSQL image runs this once, when it initialises an empty data directory, as
# POSTGRES_USER (the owner role Flyway migrates with). It is used by docker-compose.yml and by
# the Testcontainers PostgreSQL container in the backend tests.
#
# The runtime role is deliberately not a superuser, cannot bypass row-level security and owns
# nothing, so the tenant RLS policies apply to every query the application makes. Table
# privileges are granted by the Flyway migrations (see V3__grant_app_runtime_role.sql).
# The outbox worker's SECURITY DEFINER queue function is owned by a separate NOLOGIN role that
# can bypass RLS but has SELECT only on the outbox table; it exposes due society UUIDs only.
set -euo pipefail

: "${APP_DB_USER:?APP_DB_USER must name the application runtime role}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD must be set}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    --set=app_user="$APP_DB_USER" --set=app_password="$APP_DB_PASSWORD" <<'SQL'
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password'
    NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
CREATE ROLE societix_outbox_queue_reader NOLOGIN
    NOSUPERUSER BYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
SQL
