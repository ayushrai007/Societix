package com.societix.platform.notification;

import java.util.UUID;

/** Adapter for a resident notification channel. The outbox event ID is the stable delivery key. */
public interface NotificationChannel {

    String channelKey();

    void send(UUID societyId, UUID residentId, String title, String body, UUID eventId);
}
