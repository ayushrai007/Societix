package com.societix;

import static org.assertj.core.api.Assertions.assertThat;

import com.societix.platform.jobs.DueWorkJob;
import com.societix.platform.jobs.internal.DailyDueWorkScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/** SOC-22 scheduled due-work integration tests against the PostgreSQL Testcontainer. */
@SpringBootTest(properties = "societix.jobs.daily-cron=0 0 0 1 1 *")
@Import({TestcontainersConfiguration.class, ScheduledJobsTestConfiguration.class})
class ScheduledJobsIT {

    private static final String DAILY_LOCK = "societixDailyDueWork";

    private final DailyDueWorkScheduler scheduler;
    private final LockProvider lockProvider;
    private final JdbcTemplate ownerJdbc;
    private final List<DueWorkJob> dueWorkJobs;
    private final Environment environment;

    @Autowired
    ScheduledJobsIT(
            DailyDueWorkScheduler scheduler,
            LockProvider lockProvider,
            org.flywaydb.core.Flyway flyway,
            List<DueWorkJob> dueWorkJobs,
            Environment environment) {
        this.scheduler = scheduler;
        this.lockProvider = lockProvider;
        this.ownerJdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
        this.dueWorkJobs = dueWorkJobs;
        this.environment = environment;
    }

    @BeforeEach
    void resetTestDataAndExpireDailyLock() {
        ownerJdbc.update("DELETE FROM societix_scheduled_job_it_work");
        ownerJdbc.update(
                "INSERT INTO shedlock (name, lock_until, locked_at, locked_by) "
                        + "VALUES (?, now() - interval '1 second', now() - interval '2 seconds', 'test-reset') "
                        + "ON CONFLICT (name) DO UPDATE SET lock_until = EXCLUDED.lock_until, "
                        + "locked_at = EXCLUDED.locked_at, locked_by = EXCLUDED.locked_by",
                DAILY_LOCK);
    }

    @AfterEach
    void expireDailyLockWithoutDeletingItsCachedRow() {
        expireLock(DAILY_LOCK);
    }

    @Test
    void scheduledJobProcessesDueRowsAndSupportsTreasuryAndPrivacy() throws NoSuchMethodException {
        Scheduled schedule = DailyDueWorkScheduler.class
                .getMethod("runDailyDueWork")
                .getAnnotation(Scheduled.class);
        assertThat(schedule).isNotNull();
        assertThat(schedule.cron()).isEqualTo("${societix.jobs.daily-cron}");
        assertThat(environment.getProperty("societix.jobs.daily-cron")).isEqualTo("0 0 0 1 1 *");
        assertThat(schedule.zone()).isEqualTo("${societix.jobs.zone}");
        assertThat(environment.getProperty("societix.jobs.zone")).isEqualTo("UTC");

        insertWork("treasury", "1 day");
        insertWork("privacy", "1 day");
        insertWork("treasury", "-1 day");
        insertWork("privacy", "-1 day");
        assertThat(dueWorkJobs)
                .extracting(DueWorkJob::jobKey)
                .containsExactlyInAnyOrder("treasury", "privacy");
        assertThat(duePendingCount()).isEqualTo(2);

        scheduler.runDailyDueWork();

        assertThat(lockIsHeld(DAILY_LOCK)).isTrue();
        assertThat(processedCount("treasury")).isEqualTo(1);
        assertThat(processedCount("privacy")).isEqualTo(1);
        assertThat(pendingCount()).isEqualTo(2);
        assertThat(duePendingCount()).isZero();
    }

    @Test
    void scheduledJobSkipsWorkWhileAnotherInstanceHoldsItsActualLock() throws NoSuchMethodException {
        SchedulerLock annotation = DailyDueWorkScheduler.class
                .getMethod("runDailyDueWork")
                .getAnnotation(SchedulerLock.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo(DAILY_LOCK);
        Duration lockAtMostFor = Duration.parse(annotation.lockAtMostFor());
        Duration lockAtLeastFor = Duration.parse(annotation.lockAtLeastFor());
        assertThat(lockAtMostFor).isEqualTo(Duration.ofHours(1));
        assertThat(lockAtLeastFor).isEqualTo(Duration.ofMinutes(1));

        insertWork("treasury", "-1 day");
        LockConfiguration configuration = new LockConfiguration(
                Instant.now(), annotation.name(), lockAtMostFor, lockAtLeastFor);
        SimpleLock otherInstance = lockProvider.lock(configuration).orElseThrow();
        try {
            scheduler.runDailyDueWork();
            assertThat(processedCount("treasury")).isZero();
            assertThat(duePendingCount()).isEqualTo(1);
        } finally {
            otherInstance.unlock();
        }
    }

    private void insertWork(String jobKey, String offset) {
        ownerJdbc.update(
                "INSERT INTO societix_scheduled_job_it_work (job_key, due_at) VALUES (?, now() + ?::interval)",
                jobKey,
                offset);
    }

    private int processedCount(String jobKey) {
        return ownerJdbc.queryForObject(
                "SELECT count(*) FROM societix_scheduled_job_it_work WHERE job_key = ? AND processed_at IS NOT NULL",
                Integer.class,
                jobKey);
    }

    private int pendingCount() {
        return ownerJdbc.queryForObject(
                "SELECT count(*) FROM societix_scheduled_job_it_work WHERE processed_at IS NULL",
                Integer.class);
    }

    private int duePendingCount() {
        return ownerJdbc.queryForObject(
                "SELECT count(*) FROM societix_scheduled_job_it_work "
                        + "WHERE due_at <= now() AND processed_at IS NULL",
                Integer.class);
    }

    private boolean lockIsHeld(String name) {
        return ownerJdbc.queryForObject(
                "SELECT lock_until > localtimestamp FROM shedlock WHERE name = ?", Boolean.class, name);
    }

    private void expireLock(String name) {
        ownerJdbc.update(
                "UPDATE shedlock SET lock_until = now() - interval '1 second', "
                        + "locked_at = now() - interval '2 seconds' WHERE name = ?",
                name);
    }
}
