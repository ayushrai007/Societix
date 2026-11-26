package com.societix.platform.notification.internal;

import com.societix.platform.notification.NotificationChannel;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** SOC-30 adapter seam for WhatsApp. Provider transport is intentionally a stub until configured. */
@Component
public class WhatsAppNotificationChannel implements NotificationChannel {

    private final WhatsAppGateway gateway;

    public WhatsAppNotificationChannel(WhatsAppGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public String channelKey() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public void send(UUID societyId, UUID residentId, String title, String body, UUID eventId) {
        gateway.send(societyId, residentId, title, body, eventId);
    }
}
