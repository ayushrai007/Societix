package com.societix.platform.notification.internal;

import java.util.UUID;

/** Provider transport boundary for WhatsApp delivery. */
public interface WhatsAppGateway {

    void send(UUID societyId, UUID residentId, String title, String body, UUID idempotencyKey);
}
