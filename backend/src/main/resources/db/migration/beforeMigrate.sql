-- Provision the narrowly scoped owner of the outbox SECURITY DEFINER function before V5.
-- Unlike PostgreSQL initdb scripts, this callback also runs against existing data volumes.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_roles WHERE rolname = 'societix_outbox_queue_reader'
    ) THEN
        CREATE ROLE societix_outbox_queue_reader NOLOGIN
            NOSUPERUSER BYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
    ELSIF EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'societix_outbox_queue_reader'
          AND (rolsuper OR NOT rolbypassrls OR rolcanlogin)
    ) THEN
        RAISE EXCEPTION
            'societix_outbox_queue_reader must be NOLOGIN NOSUPERUSER BYPASSRLS';
    END IF;
END
$$;

-- Membership lets Flyway transfer the SECURITY DEFINER function to this NOLOGIN role.
DO $$
BEGIN
    EXECUTE format('GRANT societix_outbox_queue_reader TO %I', current_user);
END
$$;
