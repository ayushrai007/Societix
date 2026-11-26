# Outbox event contracts

The Postgres outbox is the platform's asynchronous event transport. Producers write event rows in
the same transaction as their business action; registered handlers are at-least-once and must be
idempotent. This catalogue records the payload shape and handler contract shared between modules.

## `notification.requested`

**Producer:** `platform.notification.NotificationService.notifyResident`  
**Handler key:** `platform-notification-requested-v1`  
**Purpose:** deliver one notification request through its explicitly selected channel.

Payload:

```json
{
  "residentId": "UUID",
  "channelKey": "in-app | whatsapp",
  "title": "string",
  "body": "string"
}
```

The outbox row supplies the `societyId` and stable event ID. `channelKey` names exactly one
registered adapter; the handler does not fan out across channels. The event ID is passed to the
selected adapter as its provider idempotency key. The in-app adapter stores at most one inbox row per
`(tenant_id, event_id)`; the database uniqueness constraint and RLS protect that effect. The default
WhatsApp gateway is not configured: it throws without logging message content, so the dispatcher
retries and eventually parks the event instead of marking an undelivered notification as processed.
Live WhatsApp Cloud API delivery is a follow-up requiring a resident contact and approved-template
provider contract.

The title and body are persisted in the outbox and, for in-app delivery, in `resident_notification`.
Do not put secrets or unnecessary personal data in event content, and do not log the message body.
