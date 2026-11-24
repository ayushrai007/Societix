-- Provision the narrowly scoped owner of the outbox SECURITY DEFINER function before V5.
-- Unlike PostgreSQL initdb scripts, this callback also runs against existing data volumes.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_roles WHERE rolname = 'societix_outbox_queue_reader'
    ) THEN
        BEGIN
            CREATE ROLE societix_outbox_queue_reader NOLOGIN
                NOSUPERUSER BYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
        EXCEPTION WHEN duplicate_object THEN
            -- Another application instance may have created the cluster role concurrently.
            NULL;
        END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'societix_outbox_queue_reader'
          AND (rolsuper OR NOT rolbypassrls OR rolcanlogin OR rolcreatedb OR rolcreaterole OR rolreplication)
    ) THEN
        RAISE EXCEPTION
            'societix_outbox_queue_reader must be NOLOGIN NOSUPERUSER BYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION';
    END IF;
END
$$;

-- Add explicit membership for Flyway so it can transfer function ownership. Existing membership
-- needs no new grant (which could require ADMIN OPTION on PostgreSQL 16+).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = current_user AND rolsuper)
       OR NOT pg_has_role(current_user, 'societix_outbox_queue_reader', 'MEMBER') THEN
        EXECUTE format('GRANT societix_outbox_queue_reader TO %I', current_user);
    END IF;
END
$$;
