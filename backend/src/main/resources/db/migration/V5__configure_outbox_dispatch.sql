ALTER TABLE outbox
    ADD COLUMN next_attempt_at timestamptz NOT NULL DEFAULT now(),
    ADD CONSTRAINT outbox_id_tenant_id_key UNIQUE (id, tenant_id);

DROP INDEX outbox_unprocessed_by_tenant_created_at_idx;
CREATE INDEX outbox_due_by_tenant_created_at_idx
    ON outbox (tenant_id, next_attempt_at, created_at, id)
    WHERE processed_at IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_roles
        WHERE rolname = 'societix_outbox_queue_reader'
          AND rolbypassrls
          AND NOT rolcanlogin
    ) THEN
        RAISE EXCEPTION 'Provision societix_outbox_queue_reader as NOLOGIN BYPASSRLS before applying SOC-21';
    END IF;
END
$$;

CREATE TABLE outbox_handler_receipt (
    tenant_id   uuid NOT NULL REFERENCES society (id),
    event_id    uuid NOT NULL,
    handler_key text NOT NULL,
    handled_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT outbox_handler_receipt_pk PRIMARY KEY (tenant_id, event_id, handler_key),
    CONSTRAINT outbox_handler_receipt_event_fk
        FOREIGN KEY (event_id, tenant_id) REFERENCES outbox (id, tenant_id),
    CONSTRAINT outbox_handler_receipt_handler_key_not_blank CHECK (length(btrim(handler_key)) > 0)
);

ALTER TABLE outbox_handler_receipt ENABLE ROW LEVEL SECURITY;
ALTER TABLE outbox_handler_receipt FORCE ROW LEVEL SECURITY;

CREATE POLICY outbox_handler_receipt_tenant_isolation ON outbox_handler_receipt
    USING (
        tenant_id = CASE
            WHEN current_setting('app.tenant_id', true) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                THEN current_setting('app.tenant_id', true)::uuid
            ELSE NULL::uuid
        END
    )
    WITH CHECK (
        tenant_id = CASE
            WHEN current_setting('app.tenant_id', true) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                THEN current_setting('app.tenant_id', true)::uuid
            ELSE NULL::uuid
        END
    );

GRANT UPDATE (processed_at, attempts, next_attempt_at) ON TABLE outbox TO "${app_runtime_role}";
GRANT SELECT, INSERT ON TABLE outbox_handler_receipt TO "${app_runtime_role}";

-- The worker needs a tenant work queue while the application role remains subject to RLS.
-- This SECURITY DEFINER function exposes only tenant UUIDs with due work, never event contents.
CREATE FUNCTION public.outbox_pending_tenants(p_limit integer)
RETURNS TABLE (tenant_id uuid)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
SET row_security = off
AS $function$
    SELECT pending.tenant_id
    FROM (
        SELECT o.tenant_id, min(o.created_at) AS oldest_event
        FROM public.outbox o
        WHERE o.processed_at IS NULL
          AND o.next_attempt_at <= now()
        GROUP BY o.tenant_id
    ) pending
    ORDER BY pending.oldest_event, pending.tenant_id
    LIMIT GREATEST(p_limit, 0)
$function$;

REVOKE ALL ON FUNCTION public.outbox_pending_tenants(integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.outbox_pending_tenants(integer) TO "${app_runtime_role}";
GRANT USAGE, CREATE ON SCHEMA public TO societix_outbox_queue_reader;
GRANT SELECT ON TABLE outbox TO societix_outbox_queue_reader;
ALTER FUNCTION public.outbox_pending_tenants(integer) OWNER TO societix_outbox_queue_reader;
REVOKE CREATE ON SCHEMA public FROM societix_outbox_queue_reader;

CREATE TABLE shedlock (
    name       varchar(64) PRIMARY KEY,
    lock_until timestamp NOT NULL,
    locked_at  timestamp NOT NULL,
    locked_by  varchar(255) NOT NULL
);

GRANT SELECT, INSERT, UPDATE ON TABLE shedlock TO "${app_runtime_role}";
