package com.societix;

import static org.mockito.Mockito.verify;

import com.societix.platform.notification.internal.WhatsAppGateway;
import com.societix.platform.notification.internal.WhatsAppNotificationChannel;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WhatsAppNotificationChannelTest {

    @Test
    void delegatesToWhatsAppTransportWithEventIdempotencyKey() {
        WhatsAppGateway gateway = org.mockito.Mockito.mock(WhatsAppGateway.class);
        WhatsAppNotificationChannel channel = new WhatsAppNotificationChannel(gateway);
        UUID societyId = UUID.randomUUID();
        UUID residentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        channel.send(societyId, residentId, "Notice", "Service update", eventId);

        verify(gateway).send(societyId, residentId, "Notice", "Service update", eventId);
    }
}
