-- Global Firebase identity mapping. Phone numbers are authenticated by Firebase, then stored here
-- only as authenticated ciphertext; this table is an identity directory, not tenant data.
CREATE TABLE identity_account (
    id                  uuid PRIMARY KEY,
    firebase_uid        varchar(128) NOT NULL UNIQUE,
    phone_number_cipher bytea NOT NULL,
    created_at          timestamptz NOT NULL DEFAULT now()
);

-- Platform operators are global identities and are therefore kept outside tenant RLS. This table
-- lets the request filter revoke a platform administrator immediately through a version/status change.
CREATE TABLE platform_admin_account (
    identity_id           uuid PRIMARY KEY REFERENCES identity_account (id),
    status                varchar(20) NOT NULL DEFAULT 'ACTIVE',
    authorization_version bigint NOT NULL DEFAULT 1,
    CONSTRAINT platform_admin_status_valid CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT platform_admin_version_positive CHECK (authorization_version > 0)
);

-- A pending or approved user can be associated with only one society at a time. Rejected
-- applications remain as history and permit the user to apply to another society.
CREATE TABLE society_membership (
    id                          uuid PRIMARY KEY,
    tenant_id                   uuid NOT NULL REFERENCES society (id),
    identity_id                 uuid NOT NULL REFERENCES identity_account (id),
    membership_type             varchar(20) NOT NULL,
    status                      varchar(20) NOT NULL DEFAULT 'PENDING',
    roles                       jsonb NOT NULL DEFAULT '[]'::jsonb,
    authorization_version       bigint NOT NULL DEFAULT 0,
    claims_sync_version         bigint NOT NULL DEFAULT 0,
    requested_at                timestamptz NOT NULL DEFAULT now(),
    decided_at                  timestamptz,
    decided_by_firebase_uid     varchar(128),
    CONSTRAINT society_membership_type_valid
        CHECK (membership_type IN ('RESIDENT', 'SECURITY_GUARD')),
    CONSTRAINT society_membership_status_valid
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    CONSTRAINT society_membership_roles_array
        CHECK (jsonb_typeof(roles) = 'array'),
    CONSTRAINT society_membership_authorization_version_non_negative
        CHECK (authorization_version >= 0 AND claims_sync_version >= 0)
);

CREATE UNIQUE INDEX society_membership_one_active_per_identity
    ON society_membership (identity_id)
    WHERE status IN ('PENDING', 'APPROVED', 'SUSPENDED');

CREATE INDEX society_membership_pending_by_tenant
    ON society_membership (tenant_id, requested_at, id)
    WHERE status = 'PENDING';

ALTER TABLE society_membership ENABLE ROW LEVEL SECURITY;
ALTER TABLE society_membership FORCE ROW LEVEL SECURITY;

CREATE POLICY society_membership_tenant_isolation ON society_membership
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

ALTER TABLE society ADD COLUMN initial_secretary_assigned_at timestamptz;

GRANT SELECT, INSERT, UPDATE ON TABLE identity_account TO "${app_runtime_role}";
GRANT SELECT, INSERT, UPDATE ON TABLE platform_admin_account TO "${app_runtime_role}";
GRANT SELECT, INSERT, UPDATE ON TABLE society_membership TO "${app_runtime_role}";
GRANT UPDATE (initial_secretary_assigned_at) ON TABLE society TO "${app_runtime_role}";
