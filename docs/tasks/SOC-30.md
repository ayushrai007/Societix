# SOC-30 — Notification service with channel adapters

## Delivery

Added a public Platform API for modules to enqueue resident notifications as the
`notification.requested` outbox event with one explicit `channelKey`. SOC-21 dispatches only to the
selected registered adapter. The in-app adapter persists a resident inbox item in a tenant-scoped,
RLS-protected table. A WhatsApp adapter delegates through an injectable gateway; its default gateway
is deliberately a no-op stub because this story does not define provider configuration or API
details.

## Touched files

- `backend/src/main/java/com/societix/platform/notification/package-info.java`
- `backend/src/main/java/com/societix/platform/notification/NotificationRequest.java`
- `backend/src/main/java/com/societix/platform/notification/NotificationService.java`
- `backend/src/main/java/com/societix/platform/notification/NotificationChannel.java`
- `backend/src/main/java/com/societix/platform/notification/internal/InAppNotificationChannel.java`
- `backend/src/main/java/com/societix/platform/notification/internal/NotificationRequestedHandler.java`
- `backend/src/main/java/com/societix/platform/notification/internal/WhatsAppNotificationChannel.java`
- `backend/src/main/java/com/societix/platform/notification/internal/WhatsAppGateway.java`
- `backend/src/main/java/com/societix/platform/notification/internal/StubWhatsAppGateway.java`
- `backend/src/main/resources/db/migration/V8__create_resident_notifications.sql`
- `backend/src/test/java/com/societix/NotificationServiceIT.java`
- `backend/src/test/java/com/societix/NotificationRequestedHandlerTest.java`
- `backend/src/test/java/com/societix/WhatsAppNotificationChannelTest.java`
- `backend/src/test/java/com/societix/FlywayMigrationTest.java`
- `docs/api/events.md`

## Acceptance criteria and tests

1. A module can request `notify resident X`: `NotificationServiceIT.moduleRequestIsDispatchedFromOutboxIntoResidentInbox` verifies the public request creates an outbox row.
2. In-app and external channel adapters are available: `WhatsAppNotificationChannelTest.delegatesToWhatsAppTransportWithEventIdempotencyKey` verifies adapter delegation, and `NotificationRequestedHandlerTest.sendsOnlyThroughTheChannelSelectedOnTheRequest` proves exactly the requested adapter is called. The default WhatsApp gateway remains a stub (see limits below).
3. Outbox-driven sending: `NotificationServiceIT.moduleRequestIsDispatchedFromOutboxIntoResidentInbox` verifies the notification is absent before dispatch and appears after `OutboxEventProcessor` processes the event.
4. Replay safety: `NotificationServiceIT.inAppAdapterDoesNotCreateDuplicateInboxItemWhenInvokedTwice` calls the in-app adapter twice and verifies its unique constraint / `ON CONFLICT` path creates only one row.
5. Tenant isolation: `NotificationServiceIT.residentNotificationsAreIsolatedByTenant`, `tenantCannotInsertNotificationForAnotherTenant`, and `notificationTableFailsClosedWithoutTenantAndWithMalformedTenant` verify cross-tenant reads and writes plus missing and malformed tenant contexts under the app runtime role.
6. Tenant binding: `NotificationServiceIT.notificationRequestCannotRebindAnExistingTenantTransaction` verifies a request for another society is rejected without changing the current transaction's tenant setting.
7. Migration safety: `FlywayMigrationTest.residentNotificationMigrationCreatesForcedRlsAndInboxIndex` checks migration history, columns, policy, forced RLS, and the inbox index.

## Verification

Ran on Java 25.0.2 after the review fixes:

```bash
cd backend && ./mvnw -o -Dmaven.repo.local=/tmp/societix-m2-repository verify
```

Result: **BUILD SUCCESS**. Unit tests: 21 passed. Failsafe integration tests: 23 passed, including
all 6 `NotificationServiceIT` tests. Docker access is required for Testcontainers.

## Decisions and limits

- The request API takes an explicit `societyId` so the caller can propagate the current society into
  the outbox event. If a tenant context is already present, the service rejects a mismatch before it
  writes; only an unbound transaction may be initialized from the request.
- Each request names exactly one `channelKey`; the handler does not broadcast to all adapters.
- The inbox uniqueness constraint is `(tenant_id, event_id)`; the event ID is also passed to channel
  adapters as their idempotency key.
- WhatsApp Cloud API delivery remains a follow-up. The user selected Cloud API with credentials
  configured later and confirmed that the resident model will provide the phone number and approved
  template. This baseline has no resident model or contact/template lookup contract, so this change
  retains the injectable gateway stub rather than persisting phone numbers in the outbox or inventing
  a resident data source. A real adapter must integrate with the resident model when that contract
  lands.
- `residentId` is not yet checked against a resident/society membership and has no resident FK because
  the resident model is not present in the current merged baseline.
- The notification migration is V8 because an open concurrent PR adds V7. If this PR merges before
  that PR, the later PR must rebase and move its migration above V8 to avoid an out-of-order Flyway
  upgrade; see [database migrations](../database-migrations.md).

## Local run and verification

With Docker available, run the full suite from `backend/`:

```bash
./mvnw verify
```

The Testcontainers integration tests use PostgreSQL 18 and the application's non-superuser runtime
role. `NotificationServiceIT` covers dispatch, idempotency, RLS, and tenant binding.

## Configuration and environment

No provider credentials are configured for SOC-30. The current WhatsApp gateway remains a no-op
local stub until it can resolve the resident phone and approved template through the resident model.

## Evidence

- No endpoint was added; other modules use the public `NotificationService` API.
