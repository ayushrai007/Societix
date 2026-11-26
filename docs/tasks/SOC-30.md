# SOC-30 — Notification service with channel adapters

## Delivery

Added a public Platform API for modules to enqueue resident notifications as the
`notification.requested` outbox event with one explicit `channelKey`. SOC-21 dispatches only to the
selected registered adapter. The in-app adapter persists a resident inbox item in a tenant-scoped,
RLS-protected table. WhatsApp is represented by an injectable adapter; the unconfigured gateway
fails dispatch so no undelivered notification can be marked processed.

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
- `backend/src/test/java/com/societix/NotificationServiceTest.java`
- `backend/src/test/java/com/societix/platform/notification/internal/StubWhatsAppGatewayTest.java`
- `backend/src/test/java/com/societix/WhatsAppNotificationChannelTest.java`
- `backend/src/test/java/com/societix/FlywayMigrationTest.java`
- `docs/api/events.md`

## Acceptance criteria and tests

1. A module can request `notify resident X`: `NotificationServiceIT.moduleRequestIsDispatchedFromOutboxIntoResidentInbox` verifies the public request creates an outbox row.
2. In-app delivery works; the unconfigured WhatsApp adapter fails visibly and is retried/parked: `StubWhatsAppGatewayTest.unconfiguredGatewayFailsDeliveryWithoutIncludingMessageContent` checks the fail-closed stub behavior, and `NotificationServiceTest.unsupportedChannelIsRejectedBeforeWritingToTheOutbox` checks unsupported requests.
3. Outbox-driven sending: `NotificationServiceIT.moduleRequestIsDispatchedFromOutboxIntoResidentInbox` verifies the notification is absent before dispatch and appears after `OutboxEventProcessor` processes the event.
4. Replay safety: `NotificationServiceIT.inAppAdapterDoesNotCreateDuplicateInboxItemWhenInvokedTwice` calls the in-app adapter twice and verifies its unique constraint / `ON CONFLICT` path creates only one row.
5. Tenant isolation: `NotificationServiceIT.residentNotificationsAreIsolatedByTenant`, `tenantCannotInsertNotificationForAnotherTenant`, and `notificationTableFailsClosedWithoutTenantAndWithMalformedTenant` verify cross-tenant reads and writes plus missing and malformed tenant contexts under the app runtime role.
6. Tenant binding: `NotificationServiceIT.notificationRequestCannotRebindAnExistingTenantTransaction` verifies a request for another society is rejected without changing the current transaction's tenant setting.
7. Migration safety: `FlywayMigrationTest.residentNotificationMigrationCreatesForcedRlsAndInboxIndex` checks migration history, columns, policy, forced RLS, and the inbox index.
8. Unknown event channel: `NotificationRequestedHandlerTest.unknownChannelKeyFailsInsteadOfSilentlyDroppingTheEvent` verifies malformed or stale outbox messages fail for retry rather than being acknowledged.

## Verification

Ran on Java 25.0.2 after the review fixes:

```bash
cd backend && ./mvnw -o -Dmaven.repo.local=/tmp/societix-m2-repository verify
```

After the review fixes, `./mvnw -o -Dmaven.repo.local=/tmp/societix-m2-repository verify` passed on
Java 25.0.2: 24 unit tests and 23 Failsafe integration tests, including 6 `NotificationServiceIT`
tests and 7 `FlywayMigrationTest` tests. Docker access is required for Testcontainers.

## Decisions and limits

- The request API takes an explicit `societyId` so the caller can propagate the current society into
  the outbox event. If a tenant context is already present, the service rejects a mismatch before it
  writes; only an unbound transaction may be initialized from the request.
- Each request names exactly one `channelKey`; the handler does not broadcast to all adapters.
- The inbox uniqueness constraint is `(tenant_id, event_id)`; the event ID is also passed to channel
  adapters as their idempotency key.
- WhatsApp Cloud API delivery is explicitly out of this ticket's acceptance scope. The default
  gateway throws when called so the outbox retries and eventually parks the event; it never silently
  reports delivery. Live provider delivery will require the resident contact and approved-template
  contract, which is not present in this baseline; track that work in SOC-32.
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

No provider credentials or resident contact/template provider are configured for SOC-30. Selecting
WhatsApp fails safely and the outbox retries/parks the event.

## Migration rollout and rollback

V8 is additive: it creates the inbox table, forced RLS policy, grants, and lookup index. Deploy the
application with Flyway enabled so V8 applies before notification requests are accepted. Roll back
by reverting the application release while leaving V8 in place; do not drop the table or roll back
the migration, because a later version may already have written inbox rows.

## Evidence

- No endpoint was added; other modules use the public `NotificationService` API.
