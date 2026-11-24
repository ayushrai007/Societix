CREATE TABLE resident_notification (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid NOT NULL REFERENCES society (id),
    event_id    uuid NOT NULL,
    resident_id uuid NOT NULL,
    title       text NOT NULL,
    body        text NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT resident_notification_event_fk
        FOREIGN KEY (event_id, tenant_id) REFERENCES outbox (id, tenant_id),
    CONSTRAINT resident_notification_once_per_event UNIQUE (tenant_id, event_id),
    CONSTRAINT resident_notification_title_not_blank CHECK (length(btrim(title)) > 0),
    CONSTRAINT resident_notification_body_not_blank CHECK (length(btrim(body)) > 0)
);

ALTER TABLE resident_notification ENABLE ROW LEVEL SECURITY;
ALTER TABLE resident_notification FORCE ROW LEVEL SECURITY;

CREATE POLICY resident_notification_tenant_isolation ON resident_notification
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

GRANT SELECT, INSERT ON TABLE resident_notification TO "${app_runtime_role}";
