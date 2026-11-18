-- SOC-100: integer-paise, append-only journals; period closing workflows are separate work.
CREATE TABLE accounting_period (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES society(id),
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL CHECK (ends_on >= starts_on),
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSING', 'CLOSED')),
    UNIQUE (tenant_id, id)
);

CREATE TABLE ledger_account (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES society(id),
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('ASSET', 'LIABILITY', 'EQUITY', 'INCOME', 'EXPENSE')),
    normal_side CHAR(1) NOT NULL CHECK (normal_side = CASE WHEN type IN ('ASSET', 'EXPENSE') THEN 'D' ELSE 'C' END),
    is_fund BOOLEAN NOT NULL DEFAULT FALSE CHECK (NOT is_fund OR type = 'EQUITY'),
    parent_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (tenant_id, code),
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, parent_id) REFERENCES ledger_account(tenant_id, id)
);

CREATE TABLE ledger_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES society(id),
    txn_date DATE NOT NULL,
    description TEXT NOT NULL,
    source_type TEXT NOT NULL,
    source_ref TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    reverses_txn UUID,
    period_id BIGINT NOT NULL,
    posted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    posted_by TEXT NOT NULL,
    UNIQUE (tenant_id, idempotency_key),
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, reverses_txn) REFERENCES ledger_transaction(tenant_id, id),
    FOREIGN KEY (tenant_id, period_id) REFERENCES accounting_period(tenant_id, id)
);

CREATE TABLE ledger_entry (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES society(id),
    txn_id UUID NOT NULL,
    account_id BIGINT NOT NULL,
    debit_minor BIGINT NOT NULL DEFAULT 0 CHECK (debit_minor >= 0),
    credit_minor BIGINT NOT NULL DEFAULT 0 CHECK (credit_minor >= 0),
    memo TEXT,
    CHECK ((debit_minor = 0) <> (credit_minor = 0)),
    FOREIGN KEY (tenant_id, txn_id) REFERENCES ledger_transaction(tenant_id, id),
    FOREIGN KEY (tenant_id, account_id) REFERENCES ledger_account(tenant_id, id)
);
CREATE INDEX ix_entry_account ON ledger_entry(tenant_id, account_id);
CREATE INDEX ix_entry_transaction ON ledger_entry(tenant_id, txn_id);
CREATE INDEX ix_txn_period ON ledger_transaction(tenant_id, period_id);

-- Apply SOC-10's fail-closed UUID policy to every new tenant table. USING also supplies WITH CHECK.
DO $$
DECLARE table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['accounting_period', 'ledger_account', 'ledger_transaction', 'ledger_entry'] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
        EXECUTE format($policy$CREATE POLICY tenant_isolation ON %I USING (
            tenant_id = CASE
                WHEN current_setting('app.tenant_id', true) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                THEN current_setting('app.tenant_id', true)::uuid ELSE NULL::uuid END)$policy$, table_name);
    END LOOP;
END $$;
GRANT SELECT, INSERT ON accounting_period, ledger_account, ledger_transaction, ledger_entry TO "${app_runtime_role}";
GRANT USAGE ON SEQUENCE accounting_period_id_seq, ledger_account_id_seq, ledger_entry_id_seq TO "${app_runtime_role}";

CREATE FUNCTION assert_ledger_balanced() RETURNS trigger LANGUAGE plpgsql SET search_path = public, pg_temp AS $$
DECLARE journal_id UUID;
BEGIN
    IF TG_TABLE_NAME = 'ledger_transaction' THEN journal_id := NEW.id;
    ELSE journal_id := NEW.txn_id; END IF;
    -- NUMERIC sums avoid BIGINT overflow. Empty or RLS-hidden journals must fail too.
    IF NOT EXISTS (SELECT 1 FROM ledger_entry WHERE tenant_id = NEW.tenant_id AND txn_id = journal_id
                   HAVING count(*) >= 2 AND sum(debit_minor) = sum(credit_minor)) THEN
        RAISE EXCEPTION 'ledger transaction must contain balanced debit and credit entries' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER ledger_header_balanced AFTER INSERT ON ledger_transaction
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION assert_ledger_balanced();
-- Also check later entry inserts: checking only a newly inserted header leaves a balance bypass.
CREATE CONSTRAINT TRIGGER ledger_entries_balanced AFTER INSERT ON ledger_entry
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION assert_ledger_balanced();

CREATE FUNCTION reject_ledger_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'posted ledger rows are immutable; use a reversing journal' USING ERRCODE = '23514';
END $$;
CREATE TRIGGER ledger_header_immutable BEFORE UPDATE OR DELETE ON ledger_transaction
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_mutation();
CREATE TRIGGER ledger_entries_immutable BEFORE UPDATE OR DELETE ON ledger_entry
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_mutation();
REVOKE UPDATE, DELETE, TRUNCATE ON ledger_transaction, ledger_entry FROM "${app_runtime_role}";

CREATE FUNCTION seed_ledger_funds(society_id UUID) RETURNS void LANGUAGE sql SET search_path = public, pg_temp AS $$
    INSERT INTO ledger_account(tenant_id, code, name, type, normal_side, is_fund)
    SELECT society_id, code, name, 'EQUITY', 'C', TRUE FROM (VALUES
        ('3000', 'Common Maintenance Fund'), ('3100', 'Sinking Fund'),
        ('3200', 'Repair & Maintenance Fund'), ('3300', 'Reserve Fund')
    ) AS funds(code, name) ON CONFLICT (tenant_id, code) DO NOTHING;
$$;
CREATE FUNCTION seed_new_society_ledger() RETURNS trigger LANGUAGE plpgsql SET search_path = public, pg_temp AS $$
BEGIN
    PERFORM seed_ledger_funds(NEW.tenant_id);
    RETURN NEW;
END $$;
-- SECURITY INVOKER: onboarding's existing transaction and tenant context govern these writes.
CREATE TRIGGER society_ledger_funds AFTER INSERT ON society
    FOR EACH ROW EXECUTE FUNCTION seed_new_society_ledger();

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_roles
        WHERE rolname = 'societix_outbox_queue_reader'
          AND NOT rolsuper
          AND rolbypassrls
          AND NOT rolcanlogin
    ) THEN
        RAISE EXCEPTION 'Provision societix_outbox_queue_reader as NOLOGIN NOSUPERUSER BYPASSRLS before applying SOC-100';
    END IF;
END $$;

CREATE FUNCTION backfill_ledger_funds() RETURNS void
LANGUAGE sql
SECURITY DEFINER
SET search_path = pg_catalog, public
SET row_security = off
AS $$
    INSERT INTO public.ledger_account(tenant_id, code, name, type, normal_side, is_fund)
    SELECT society.id, funds.code, funds.name, 'EQUITY', 'C', TRUE
    FROM public.society
    CROSS JOIN (VALUES
        ('3000', 'Common Maintenance Fund'), ('3100', 'Sinking Fund'),
        ('3200', 'Repair & Maintenance Fund'), ('3300', 'Reserve Fund')
    ) AS funds(code, name)
    ON CONFLICT (tenant_id, code) DO NOTHING;
$$;
REVOKE ALL ON FUNCTION backfill_ledger_funds() FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO societix_outbox_queue_reader;
GRANT SELECT ON society TO societix_outbox_queue_reader;
GRANT INSERT ON ledger_account TO societix_outbox_queue_reader;
GRANT USAGE ON SEQUENCE ledger_account_id_seq TO societix_outbox_queue_reader;
ALTER FUNCTION backfill_ledger_funds() OWNER TO societix_outbox_queue_reader;
DO $$ BEGIN
    EXECUTE format('GRANT EXECUTE ON FUNCTION backfill_ledger_funds() TO %I', session_user);
END $$;
SELECT backfill_ledger_funds();
DROP FUNCTION backfill_ledger_funds();
REVOKE SELECT ON society FROM societix_outbox_queue_reader;
REVOKE INSERT ON ledger_account FROM societix_outbox_queue_reader;
REVOKE USAGE ON SEQUENCE ledger_account_id_seq FROM societix_outbox_queue_reader;
