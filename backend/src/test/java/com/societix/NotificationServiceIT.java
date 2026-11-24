package com.societix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.societix.platform.notification.NotificationRequest;
import com.societix.platform.notification.NotificationService;
import com.societix.platform.notification.internal.InAppNotificationChannel;
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
    private final InAppNotificationChannel inAppChannel;

    @Autowired
    NotificationServiceIT(
            NotificationService notificationService,
            OutboxEventProcessor processor,
            SocietyOnboardingService onboarding,
            JdbcTemplate jdbcTemplate,
            InAppNotificationChannel inAppChannel,
            PlatformTransactionManager transactionManager) {
        this.notificationService = notificationService;
        this.processor = processor;
        this.onboarding = onboarding;
        this.jdbcTemplate = jdbcTemplate;
        this.inAppChannel = inAppChannel;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** AC1 + AC3: a module request is stored in the outbox and delivered by the outbox processor. */
    @Test
    void moduleRequestIsDispatchedFromOutboxIntoResidentInbox() {
        Society society = createSociety("Notification delivery");
        UUID residentId = UUID.randomUUID();
        UUID eventId = notificationService.notifyResident(
                new NotificationRequest(society.getId(), residentId, "in-app", "Notice", "Water service at 3 PM"));

        assertThat(countOutboxEvents(society.getId(), eventId)).isEqualTo(1);
        assertThat(countNotifications(society.getId(), eventId)).isZero();

        assertThat(processor.process(society.getId(), eventId)).isTrue();
        assertThat(notificationFor(society.getId(), eventId))
                .containsExactly(residentId, "Notice", "Water service at 3 PM");
    }

    /** The database idempotency constraint holds even if the in-app adapter is invoked repeatedly. */
    @Test
    void inAppAdapterDoesNotCreateDuplicateInboxItemWhenInvokedTwice() {
        Society society = createSociety("Notification idempotency");
        UUID residentId = UUID.randomUUID();
        UUID eventId = notificationService.notifyResident(new NotificationRequest(
                society.getId(), residentId, "in-app", "Notice", "Building maintenance"));

        assertThat(processor.process(society.getId(), eventId)).isTrue();
        inTenant(society.getId(), () -> {
            inAppChannel.send(society.getId(), residentId, "Notice", "Building maintenance", eventId);
            return null;
        });
        inTenant(society.getId(), () -> {
            inAppChannel.send(society.getId(), residentId, "Notice", "Building maintenance", eventId);
            return null;
        });

        assertThat(countNotifications(society.getId(), eventId)).isEqualTo(1);
    }

    /** Tenant table requirement: RLS hides tenant A's inbox item while tenant B is active. */
    @Test
    void residentNotificationsAreIsolatedByTenant() {
        Society societyA = createSociety("Notification tenant A");
        Society societyB = createSociety("Notification tenant B");
        UUID eventId = notificationService.notifyResident(
                new NotificationRequest(societyA.getId(), UUID.randomUUID(), "in-app", "Private", "Tenant A only"));
        assertThat(processor.process(societyA.getId(), eventId)).isTrue();
        assertThat(countNotifications(societyA.getId(), eventId)).isEqualTo(1);

        assertThat(countNotifications(societyB.getId(), eventId)).isZero();
    }

    @Test
    void tenantCannotInsertNotificationForAnotherTenant() {
        Society societyA = createSociety("Notification write tenant A");
        Society societyB = createSociety("Notification write tenant B");
        UUID eventId = notificationService.notifyResident(new NotificationRequest(
                societyA.getId(), UUID.randomUUID(), "in-app", "Notice", "Tenant A event"));

        assertThatThrownBy(() -> inTenant(societyB.getId(), () -> jdbcTemplate.update(
                        "insert into resident_notification "
                                + "(tenant_id, event_id, resident_id, title, body) values (?, ?, ?, ?, ?)",
                        societyA.getId(),
                        eventId,
                        UUID.randomUUID(),
                        "Injected",
                        "Tenant B cannot write for tenant A")))
                .satisfies(error -> assertThat(error.getCause()).hasMessageContaining("row-level security"));
    }

    @Test
    void notificationTableFailsClosedWithoutTenantAndWithMalformedTenant() {
        Society society = createSociety("Notification fail closed");
        UUID eventId = notificationService.notifyResident(new NotificationRequest(
                society.getId(), UUID.randomUUID(), "in-app", "Notice", "Fail closed"));

        assertThat(countWithoutTenant()).isZero();
        assertThatThrownBy(() -> insertNotificationWithoutTenant(society.getId(), eventId))
                .satisfies(error -> assertThat(error.getCause()).hasMessageContaining("row-level security"));
        assertThat(countWithTenantValue("not-a-uuid")).isZero();
        assertThatThrownBy(() -> insertNotificationWithTenantValue(
                        society.getId(), eventId, "not-a-uuid"))
                .satisfies(error -> assertThat(error.getCause()).hasMessageContaining("row-level security"));
    }

    @Test
    void notificationRequestCannotRebindAnExistingTenantTransaction() {
        Society societyA = createSociety("Notification binding tenant A");
        Society societyB = createSociety("Notification binding tenant B");
        transactions.executeWithoutResult(status -> {
            setTenant(societyA.getId());
            assertThatThrownBy(() -> notificationService.notifyResident(new NotificationRequest(
                            societyB.getId(), UUID.randomUUID(), "in-app", "Notice", "Wrong tenant")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("does not match");
            assertThat(currentTenant()).isEqualTo(societyA.getId().toString());
            status.setRollbackOnly();
        });
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

    private int countWithoutTenant() {
        return transactions.execute(status -> jdbcTemplate.queryForObject(
                "select count(*) from resident_notification", Integer.class));
    }

    private int countWithTenantValue(String tenantValue) {
        return transactions.execute(status -> {
            setTenantValue(tenantValue);
            return jdbcTemplate.queryForObject("select count(*) from resident_notification", Integer.class);
        });
    }

    private void insertNotificationWithoutTenant(UUID societyId, UUID eventId) {
        transactions.executeWithoutResult(status -> jdbcTemplate.update(
                "insert into resident_notification "
                        + "(tenant_id, event_id, resident_id, title, body) values (?, ?, ?, ?, ?)",
                societyId,
                eventId,
                UUID.randomUUID(),
                "No tenant",
                "Must fail closed"));
    }

    private void insertNotificationWithTenantValue(UUID societyId, UUID eventId, String tenantValue) {
        transactions.executeWithoutResult(status -> {
            setTenantValue(tenantValue);
            jdbcTemplate.update(
                    "insert into resident_notification "
                            + "(tenant_id, event_id, resident_id, title, body) values (?, ?, ?, ?, ?)",
                    societyId,
                    eventId,
                    UUID.randomUUID(),
                    "Malformed tenant",
                    "Must fail closed");
        });
    }

    private String currentTenant() {
        return jdbcTemplate.queryForObject("select current_setting('app.tenant_id', true)", String.class);
    }

    private void setTenant(UUID societyId) {
        setTenantValue(societyId.toString());
    }

    private void setTenantValue(String tenantValue) {
        jdbcTemplate.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, tenantValue);
    }

    private <T> T inTenant(UUID societyId, java.util.function.Supplier<T> work) {
        return transactions.execute(status -> {
            jdbcTemplate.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
            return work.get();
        });
    }
}
