# SOC-22 — Scheduled jobs for time-based automation

## Delivery

SOC-22 adds a reusable `DueWorkJob` contract and a daily scheduler that invokes module-owned jobs.
Spring scheduling triggers it at the configured cron and zone. ShedLock ensures only one application
instance runs a given schedule at a time. Each job remains responsible for its own query, short
transaction, tenant context, and idempotency.

## Acceptance criteria and tests

| Acceptance criterion | Evidence |
|---|---|
| A scheduled job finds and processes rows that are due, leaving future rows pending. | `ScheduledJobsIT.scheduledJobProcessesDueRowsAndSupportsTreasuryAndPrivacy` inserts due and future rows for both sample contributors, invokes the Spring scheduler proxy, and checks processed and pending counts. |
| The mechanism is reusable by Treasury and Privacy. | The same integration test checks that two independently keyed `DueWorkJob` beans are registered and both process their own due rows. |
| The job runs once per schedule across multiple instances. | `ScheduledJobsIT.scheduledJobSkipsWorkWhileAnotherInstanceHoldsItsActualLock` holds the scheduler's actual ShedLock name through `LockProvider`, calls the proxied scheduler, and verifies work remains pending. It also checks the configured lock durations. |

## Files touched

- `backend/src/main/java/com/societix/platform/jobs/DueWorkJob.java`
- `backend/src/main/java/com/societix/platform/jobs/internal/DailyDueWorkScheduler.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/migration/beforeMigrate.sql` and `infra/postgres/initdb/10-create-app-runtime-role.sh` (SOC-21 prerequisite provisioning currently included in this PR)
- `backend/src/test/java/com/societix/ScheduledJobsIT.java`
- `backend/src/test/java/com/societix/ScheduledJobsTestConfiguration.java`
- `backend/src/test/java/com/societix/FlywayMigrationTest.java`
- `.env.example`, `docs/01-start-here.md`, `docs/database-migrations.md`, `docs/standards/getting-started.md`
- `docs/tasks/SOC-22.md`

## Configuration and timezone decision

- `SOCIETIX_JOBS_DAILY_CRON` defaults to `0 0 2 * * *` (02:00).
- `SOCIETIX_JOBS_ZONE` defaults to `UTC`, so the default execution is 02:00 UTC.
- Operators who want 02:00 India local time must set `SOCIETIX_JOBS_ZONE=Asia/Kolkata` explicitly. No business requirement in SOC-22 selects local time over UTC, so UTC remains the predictable default.
- The test overrides the cron to a far-future date and confirms the resolved test cron and zone.

## Verification

At commit `c269545`, the PR reviewer reported `ScheduledJobsIT` passing (2 tests, 0 failures) and
`./mvnw -B verify` green (18 Surefire and 19 Failsafe tests). This review-fix revision adds test and
SQL changes after that result; those changes have not yet been run through the backend verification.

Suggested verification from `backend/`:

```bash
./mvnw -B verify
```

## Decisions and follow-ups

- ShedLock rows are retained and expired by moving `lock_until` and `locked_at` into the past. Tests do not delete rows that ShedLock may have cached.
- Cross-tenant scheduled work cannot rely on a request's tenant context. A job touching tenant tables must use a transaction and set `app.tenant_id`; work that enumerates tenants should follow `OutboxTenantBatchReader` and the `outbox_pending_tenants` SECURITY DEFINER function pattern. The test fixture uses a non-tenant work table and does not substitute for a future tenant-isolation integration test.
- The SOC-21 role provisioning prerequisite remains in this PR per the requested scope. Its callback explicitly validates least-privilege attributes, handles concurrent role creation, and avoids repeated membership grants when the migration login is already a member.
- Follow-up: add a production-level integration test for a concrete Treasury or Privacy contributor against an RLS-protected tenant table when either module owns such a due-work query.
