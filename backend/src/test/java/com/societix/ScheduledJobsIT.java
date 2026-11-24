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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/** SOC-22 scheduled due-work integration tests against the PostgreSQL Testcontainer. */
@SpringBootTest(properties = "societix.jobs.daily-cron=0 0 0 1 1 *")
@Import({TestcontainersConfiguration.class, ScheduledJobsTestConfiguration.class})
class ScheduledJobsIT {

    private final DailyDueWorkScheduler scheduler;
    private final LockProvider lockProvider;
    private final JdbcTemplate ownerJdbc;
    private final List<DueWorkJob> dueWorkJobs;

    @Autowired
    ScheduledJobsIT(
            DailyDueWorkScheduler scheduler,
            LockProvider lockProvider,
            org.flywaydb.core.Flyway flyway,
            List<DueWorkJob> dueWorkJobs) {
        this.scheduler = scheduler;
        this.lockProvider = lockProvider;
        this.ownerJdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
        this.dueWorkJobs = dueWorkJobs;
    }

    @BeforeEach
    void clearTestRows() {
        ownerJdbc.update("DELETE FROM societix_scheduled_job_it_work");
        ownerJdbc.update("DELETE FROM shedlock WHERE name = ?", "societixDailyDueWork");
    }

    @AfterEach
    void clearDailyLock() {
        ownerJdbc.update("DELETE FROM shedlock WHERE name = ?", "societixDailyDueWork");
    }

    @Test
    void scheduledJobProcessesOnlyRowsWhoseDueTimeHasArrived() throws NoSuchMethodException {
        Scheduled schedule = DailyDueWorkScheduler.class
                .getMethod("runDailyDueWork")
                .getAnnotation(Scheduled.class);
        assertThat(schedule).isNotNull();
        assertThat(schedule.cron()).isEqualTo("${societix.jobs.daily-cron:0 0 2 * * *}");

        insertWork("treasury", "1 day");
        insertWork("privacy", "1 day");
        insertWork("treasury", "-1 day");
        insertWork("privacy", "-1 day");
        assertThat(dueWorkJobs)
                .extracting(DueWorkJob::jobKey)
                .containsExactlyInAnyOrder("treasury", "privacy");
        assertThat(duePendingCount()).isEqualTo(2);

        scheduler.runDailyDueWork();

        assertThat(processedCount("treasury")).isEqualTo(1);
        assertThat(processedCount("privacy")).isEqualTo(1);
        assertThat(pendingCount()).isEqualTo(2);
        assertThat(duePendingCount()).isZero();
    }

    @Test
    void shedLockAllowsOnlyOneInstanceToAcquireDailySchedule() throws NoSuchMethodException {
        SchedulerLock annotation = DailyDueWorkScheduler.class
                .getMethod("runDailyDueWork")
                .getAnnotation(SchedulerLock.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("societixDailyDueWork");
        assertThat(annotation.lockAtLeastFor()).isEqualTo("PT1M");

        LockConfiguration configuration = new LockConfiguration(
                Instant.now(), "societixDailyDueWork", Duration.ofHours(1), Duration.ofMinutes(1));
        SimpleLock firstInstance = lockProvider.lock(configuration).orElseThrow();
        try {
            assertThat(lockProvider.lock(configuration)).isEmpty();
        } finally {
            firstInstance.unlock();
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
}
