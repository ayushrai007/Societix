package com.societix.platform.notification.internal;

import java.util.UUID;
import org.springframework.stereotype.Component;

/** No-provider local stub. It accepts the delivery call without logging or persisting message content. */
@Component
class StubWhatsAppGateway implements WhatsAppGateway {

    @Override
    public void send(UUID societyId, UUID residentId, String title, String body, UUID idempotencyKey) {
        // Provider integration is intentionally deferred; this keeps the adapter injectable and replaceable.
    }
}
