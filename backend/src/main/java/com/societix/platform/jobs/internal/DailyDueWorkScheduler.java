package com.societix.platform.jobs.internal;

import com.societix.platform.jobs.DueWorkJob;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs each module's due-work query once per daily schedule across application instances. */
@Component
public class DailyDueWorkScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DailyDueWorkScheduler.class);
    static final String LOCK_NAME = "societixDailyDueWork";

    private final List<DueWorkJob> jobs;

    public DailyDueWorkScheduler(List<DueWorkJob> jobs) {
        validateJobKeys(jobs);
        this.jobs = List.copyOf(jobs);
    }

    @Scheduled(cron = "${societix.jobs.daily-cron}", zone = "${societix.jobs.zone}")
    @SchedulerLock(name = LOCK_NAME, lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
    public void runDailyDueWork() {
        LockAssert.assertLocked();
        for (DueWorkJob job : jobs) {
            try {
                job.runDueWork();
            } catch (RuntimeException exception) {
                logger.atError()
                        .addKeyValue("jobKey", job.jobKey())
                        .addKeyValue("failureType", exception.getClass().getSimpleName())
                        .log("Scheduled due-work job failed");
            }
        }
    }

    private void validateJobKeys(List<DueWorkJob> jobs) {
        Set<String> keys = new HashSet<>();
        for (DueWorkJob job : jobs) {
            String key = job.jobKey();
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("Scheduled due-work job key must not be blank");
            }
            if (!keys.add(key)) {
                throw new IllegalStateException("Duplicate scheduled due-work job key: " + key);
            }
        }
    }
}
