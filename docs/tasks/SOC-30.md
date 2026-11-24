# SOC-30 — Notification service with channel adapters

## Delivery

Added a public Platform API for modules to enqueue resident notifications as the
`notification.requested` outbox event. SOC-21 dispatches the event to registered channel adapters.
The in-app adapter persists a resident inbox item in a tenant-scoped, RLS-protected table. A
WhatsApp adapter delegates through an injectable gateway; its default gateway is deliberately a
no-op stub because this story does not define provider configuration or API details.

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
- `backend/src/main/resources/db/migration/V7__create_resident_notifications.sql`
- `backend/src/test/java/com/societix/NotificationServiceIT.java`
- `backend/src/test/java/com/societix/WhatsAppNotificationChannelTest.java`

## Acceptance criteria and tests

1. A module can request `notify resident X`: `NotificationServiceIT.moduleRequestIsDispatchedFromOutboxIntoResidentInbox` verifies the public request creates an outbox row.
2. In-app and external channel adapters are available: `NotificationServiceIT.notificationHandlerHasInAppAndWhatsAppAdapters` checks both adapters are registered, and `WhatsAppNotificationChannelTest.delegatesToWhatsAppTransportWithEventIdempotencyKey` verifies the external adapter forwards the request and stable event ID to its gateway.
3. Outbox-driven sending: `NotificationServiceIT.moduleRequestIsDispatchedFromOutboxIntoResidentInbox` verifies the notification is absent before dispatch and appears after `OutboxEventProcessor` processes the event.
4. Replay safety: `NotificationServiceIT.replayedNotificationEventDoesNotCreateDuplicateInboxItem` verifies reprocessing does not create a second inbox record.
5. Tenant isolation: `NotificationServiceIT.residentNotificationsAreIsolatedByTenant` verifies another society cannot read the inbox row under the app runtime role.

## Verification

Ran on Java 25.0.2:

```bash
cd backend && ./mvnw -o -Dmaven.repo.local=/tmp/societix-m2-repository verify
```

Result: **BUILD SUCCESS**. Unit tests: 19 passed. Failsafe integration tests: 21 passed, including
all 4 `NotificationServiceIT` tests. Docker access was required for Testcontainers.

## Decisions and limits

- The request API takes an explicit `societyId` so the caller can propagate the current society into
  the outbox event. The service establishes `app.tenant_id` before writing under RLS.
- The inbox uniqueness constraint is `(tenant_id, event_id)`; the event ID is also passed to channel
  adapters as their idempotency key.
- WhatsApp provider delivery is **not live**: the default gateway is a no-op stub. The adapter and
  test seam are ready to connect a provider, but a real external notification provider is not yet
  demonstrably delivering. Provider selection and configuration remain a follow-up needed to satisfy
  the literal “one external channel work” criterion.
- No endpoint was added; other modules use the public `NotificationService` API.
