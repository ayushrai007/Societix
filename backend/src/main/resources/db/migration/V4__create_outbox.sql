CREATE TABLE outbox (
    id          uuid PRIMARY KEY,
    tenant_id   uuid NOT NULL REFERENCES society (id),
    event_type  text NOT NULL,
    payload     jsonb NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    processed_at timestamptz,
    attempts    integer NOT NULL DEFAULT 0,
    CONSTRAINT outbox_event_type_not_blank CHECK (length(btrim(event_type)) > 0),
    CONSTRAINT outbox_attempts_non_negative CHECK (attempts >= 0)
);

CREATE INDEX outbox_unprocessed_by_tenant_created_at_idx
    ON outbox (tenant_id, created_at, id)
    WHERE processed_at IS NULL;

ALTER TABLE outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE outbox FORCE ROW LEVEL SECURITY;

CREATE POLICY outbox_tenant_isolation ON outbox
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

GRANT SELECT, INSERT ON TABLE outbox TO "${app_runtime_role}";