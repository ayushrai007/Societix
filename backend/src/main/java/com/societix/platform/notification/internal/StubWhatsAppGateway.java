package com.societix.platform.notification.internal;

import java.util.UUID;
import org.springframework.stereotype.Component;

/** No-provider local stub. It fails delivery so the outbox retries and eventually parks the event. */
@Component
class StubWhatsAppGateway implements WhatsAppGateway {

    @Override
    public void send(UUID societyId, UUID residentId, String title, String body, UUID idempotencyKey) {
        throw new IllegalStateException("WhatsApp delivery is not configured");
    }
}
