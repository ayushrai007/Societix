-- V3: let the application's runtime role use the society table without bypassing RLS.
--
-- The application connects at runtime as a separate login role that is not a superuser, does
-- not have BYPASSRLS and does not own any table, so the RLS policies created in V2 apply to it.
-- Flyway keeps connecting as the owner role that runs these migrations.
--
-- Local and test databases create the login role (with its password) in
-- infra/postgres/initdb/10-create-app-runtime-role.sh before Flyway runs. Other environments
-- provision it the same way. The NOLOGIN fallback below only keeps this migration from failing
-- where that has not happened yet; such a role cannot be used to connect until it is provisioned.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${app_runtime_role}') THEN
        CREATE ROLE "${app_runtime_role}" NOLOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO "${app_runtime_role}";
GRANT SELECT, INSERT ON TABLE society TO "${app_runtime_role}";
