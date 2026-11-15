ALTER TABLE outbox
    ADD COLUMN failed_at timestamptz;

GRANT UPDATE (failed_at) ON TABLE outbox TO "${app_runtime_role}";

CREATE OR REPLACE FUNCTION public.outbox_pending_tenants(p_limit integer)
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
          AND o.failed_at IS NULL
          AND o.next_attempt_at <= now()
        GROUP BY o.tenant_id
    ) pending
    ORDER BY pending.oldest_event, pending.tenant_id
    LIMIT GREATEST(p_limit, 0)
$function$;
