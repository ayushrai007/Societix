package com.societix;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.societix.platform.notification.NotificationChannel;
import com.societix.platform.notification.NotificationService;
import com.societix.platform.notification.internal.NotificationRequestedHandler;
import com.societix.platform.outbox.OutboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class NotificationRequestedHandlerTest {

    @Test
    void sendsOnlyThroughTheChannelSelectedOnTheRequest() {
        NotificationChannel inApp = mock(NotificationChannel.class);
        NotificationChannel whatsapp = mock(NotificationChannel.class);
        when(inApp.channelKey()).thenReturn("in-app");
        when(whatsapp.channelKey()).thenReturn("whatsapp");
        NotificationRequestedHandler handler = new NotificationRequestedHandler(List.of(inApp, whatsapp));

        UUID eventId = UUID.randomUUID();
        UUID societyId = UUID.randomUUID();
        UUID residentId = UUID.randomUUID();
        var payload = JsonMapper.builder().build().createObjectNode()
                .put("residentId", residentId.toString())
                .put("channelKey", "whatsapp")
                .put("title", "Notice")
                .put("body", "Service update");

        handler.handle(new OutboxMessage(
                eventId, societyId, NotificationService.EVENT_TYPE, payload, Instant.now()));

        verify(whatsapp).send(societyId, residentId, "Notice", "Service update", eventId);
        verify(inApp, never()).send(societyId, residentId, "Notice", "Service update", eventId);
    }
}
