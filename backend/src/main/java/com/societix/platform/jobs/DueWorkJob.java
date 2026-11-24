package com.societix.platform.jobs;

/**
 * A module-owned job that queries and processes work that is due now.
 *
 * <p>Implementations own their query, transaction boundaries, tenant scoping, and idempotency.
 * Every database operation must run in a transaction. Tenant-scoped jobs must set
 * {@code app.tenant_id} for that transaction; a scheduler has no request tenant context. Jobs that
 * process work across tenants should follow {@code OutboxTenantBatchReader} and the
 * {@code outbox_pending_tenants} SECURITY DEFINER function pattern rather than bypassing RLS.
 * Implementations should process only due rows so Treasury and Privacy can share the platform
 * scheduler without sharing persistence models.
 */
public interface DueWorkJob {

    /** Stable key used for structured operational logs. */
    String jobKey();

    /** Query and process this module's due rows. */
    void runDueWork();
}
