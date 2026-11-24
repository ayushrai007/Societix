package com.societix;

import com.societix.platform.jobs.DueWorkJob;
import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Database-backed Treasury and Privacy contributors used to verify the shared jobs contract. */
@TestConfiguration(proxyBeanMethods = false)
class ScheduledJobsTestConfiguration {

    @Bean
    ScheduledJobsTestTable scheduledJobsTestTable(Flyway flyway) {
        JdbcTemplate ownerJdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
        ownerJdbc.execute("""
                CREATE TABLE IF NOT EXISTS societix_scheduled_job_it_work (
                    job_key text NOT NULL,
                    due_at timestamptz NOT NULL,
                    processed_at timestamptz,
                    PRIMARY KEY (job_key, due_at)
                )
                """);
        ownerJdbc.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON societix_scheduled_job_it_work TO societix_app");
        return new ScheduledJobsTestTable();
    }

    @Bean
    @DependsOn("scheduledJobsTestTable")
    DueWorkJob treasuryDueWorkJob(JdbcTemplate jdbcTemplate) {
        return new JdbcDueWorkJob("treasury", jdbcTemplate);
    }

    @Bean
    @DependsOn("scheduledJobsTestTable")
    DueWorkJob privacyDueWorkJob(JdbcTemplate jdbcTemplate) {
        return new JdbcDueWorkJob("privacy", jdbcTemplate);
    }

    static final class ScheduledJobsTestTable {}

    static class JdbcDueWorkJob implements DueWorkJob {

        private final String jobKey;
        private final JdbcTemplate jdbcTemplate;

        JdbcDueWorkJob(String jobKey, JdbcTemplate jdbcTemplate) {
            this.jobKey = jobKey;
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public String jobKey() {
            return jobKey;
        }

        @Override
        @Transactional
        public void runDueWork() {
            jdbcTemplate.update(
                    """
                    UPDATE societix_scheduled_job_it_work
                    SET processed_at = now()
                    WHERE job_key = ? AND due_at <= now() AND processed_at IS NULL
                    """,
                    jobKey);
        }
    }
}
