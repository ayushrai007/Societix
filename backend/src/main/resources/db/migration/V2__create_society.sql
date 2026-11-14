CREATE TABLE society (
    id          uuid PRIMARY KEY,
    tenant_id   uuid NOT NULL,
    name        varchar(200) NOT NULL,
    address     varchar(1000) NOT NULL,
    state       varchar(2) NOT NULL,
    CONSTRAINT society_tenant_identity CHECK (tenant_id = id),
    CONSTRAINT society_state_format CHECK (state ~ '^[A-Z]{2}$')
);

ALTER TABLE society ENABLE ROW LEVEL SECURITY;
ALTER TABLE society FORCE ROW LEVEL SECURITY;

CREATE POLICY society_tenant_isolation ON society
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
