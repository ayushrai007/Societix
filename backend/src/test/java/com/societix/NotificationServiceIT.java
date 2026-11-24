package com.societix;

import static org.assertj.core.api.Assertions.assertThat;

import com.societix.platform.notification.NotificationRequest;
import com.societix.platform.notification.NotificationChannel;
import com.societix.platform.notification.NotificationService;
import com.societix.platform.outbox.internal.OutboxEventProcessor;
import com.societix.platform.society.api.CreateSocietyRequest;
import com.societix.platform.society.domain.Society;
import com.societix.platform.society.internal.SocietyOnboardingService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** SOC-30 acceptance and RLS checks using the real PostgreSQL runtime role. */
@SpringBootTest(properties = "societix.outbox.poller.enabled=false")
@Import(TestcontainersConfiguration.class)
class NotificationServiceIT {

    private final NotificationService notificationService;
    private final OutboxEventProcessor processor;
    private final SocietyOnboardingService onboarding;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactions;
    private final java.util.List<NotificationChannel> channels;

    @Autowired
    NotificationServiceIT(
            NotificationService notificationService,
            OutboxEventProcessor processor,
            SocietyOnboardingService onboarding,
            JdbcTemplate jdbcTemplate,
            java.util.List<NotificationChannel> channels,
            PlatformTransactionManager transactionManager) {
        this.notificationService = notificationService;
        this.processor = processor;
        this.onboarding = onboarding;
        this.jdbcTemplate = jdbcTemplate;
        this.channels = channels;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** AC1 + AC3: a module request is stored in the outbox and delivered by the outbox processor. */
    @Test
    void moduleRequestIsDispatchedFromOutboxIntoResidentInbox() {
        Society society = createSociety("Notification delivery");
        UUID residentId = UUID.randomUUID();
        UUID eventId = notificationService.notifyResident(
                new NotificationRequest(society.getId(), residentId, "Notice", "Water service at 3 PM"));

        assertThat(countOutboxEvents(society.getId(), eventId)).isEqualTo(1);
        assertThat(countNotifications(society.getId(), eventId)).isZero();

        assertThat(processor.process(society.getId(), eventId)).isTrue();
        assertThat(notificationFor(society.getId(), eventId))
                .containsExactly(residentId, "Notice", "Water service at 3 PM");
    }

    /** AC2: the outbox handler has an in-app channel and a WhatsApp adapter registered in the app context. */
    @Test
    void notificationHandlerHasInAppAndWhatsAppAdapters() {
        assertThat(applicationChannels()).contains("in-app", "whatsapp");
    }

    /** SOC-21 receipt + unique constraint: replaying the event does not create a duplicate inbox item. */
    @Test
    void replayedNotificationEventDoesNotCreateDuplicateInboxItem() {
        Society society = createSociety("Notification idempotency");
        UUID eventId = notificationService.notifyResident(new NotificationRequest(
                society.getId(), UUID.randomUUID(), "Notice", "Building maintenance"));

        assertThat(processor.process(society.getId(), eventId)).isTrue();
        resetProcessedAt(society.getId(), eventId);
        assertThat(processor.process(society.getId(), eventId)).isTrue();

        assertThat(countNotifications(society.getId(), eventId)).isEqualTo(1);
    }

    /** Tenant table requirement: RLS hides tenant A's inbox item while tenant B is active. */
    @Test
    void residentNotificationsAreIsolatedByTenant() {
        Society societyA = createSociety("Notification tenant A");
        Society societyB = createSociety("Notification tenant B");
        UUID eventId = notificationService.notifyResident(
                new NotificationRequest(societyA.getId(), UUID.randomUUID(), "Private", "Tenant A only"));
        processor.process(societyA.getId(), eventId);

        assertThat(countNotifications(societyB.getId(), eventId)).isZero();
    }

    private Society createSociety(String prefix) {
        return onboarding.onboard(new CreateSocietyRequest(
                prefix + " " + UUID.randomUUID(), "5 Notification Road, Pune", "MH"));
    }

    private int countOutboxEvents(UUID societyId, UUID eventId) {
        return inTenant(societyId, () -> jdbcTemplate.queryForObject(
                "select count(*) from outbox where id = ?", Integer.class, eventId));
    }

    private int countNotifications(UUID societyId, UUID eventId) {
        return inTenant(societyId, () -> jdbcTemplate.queryForObject(
                "select count(*) from resident_notification where event_id = ?", Integer.class, eventId));
    }

    private java.util.List<Object> notificationFor(UUID societyId, UUID eventId) {
        return inTenant(societyId, () -> jdbcTemplate.queryForList(
                "select resident_id, title, body from resident_notification where event_id = ?",
                eventId).stream().flatMap(row -> java.util.stream.Stream.of(
                        row.get("resident_id"), row.get("title"), row.get("body"))).toList());
    }

    private java.util.List<String> applicationChannels() {
        return channels.stream().map(NotificationChannel::channelKey).toList();
    }

    private void resetProcessedAt(UUID societyId, UUID eventId) {
        inTenant(societyId, () -> jdbcTemplate.update(
                "update outbox set processed_at = null where id = ?", eventId));
    }

    private <T> T inTenant(UUID societyId, java.util.function.Supplier<T> work) {
        return transactions.execute(status -> {
            jdbcTemplate.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
            return work.get();
        });
    }
}
