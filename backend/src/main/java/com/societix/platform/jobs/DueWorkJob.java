package com.societix.platform.jobs;

/**
 * A module-owned job that queries and processes work that is due now.
 *
 * <p>Implementations own their query, tenant scoping, transaction boundaries, and idempotency.
 * They should process only due rows so Treasury and Privacy can share the platform scheduler
 * without sharing persistence models.
 */
public interface DueWorkJob {

    /** Stable key used for structured operational logs. */
    String jobKey();

    /** Query and process this module's due rows. */
    void runDueWork();
}
