package com.societix;

import com.societix.platform.jobs.DueWorkJob;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Database-backed Treasury and Privacy contributors used to verify the shared jobs contract. */
@TestConfiguration(proxyBeanMethods = false)
class ScheduledJobsTestConfiguration {

    static final UUID TENANT_A = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    static final UUID TENANT_B = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    static final List<UUID> TENANTS = List.of(TENANT_A, TENANT_B);

    @Bean
    ScheduledJobsTestTable scheduledJobsTestTable(Flyway flyway) {
        JdbcTemplate ownerJdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
        ownerJdbc.execute("""
                CREATE TABLE IF NOT EXISTS societix_scheduled_job_it_work (
                    tenant_id uuid NOT NULL,
                    job_key text NOT NULL,
                    due_at timestamptz NOT NULL,
                    processed_at timestamptz,
                    PRIMARY KEY (tenant_id, job_key, due_at)
                )
                """);
        ownerJdbc.execute("DROP POLICY IF EXISTS societix_scheduled_job_it_work_tenant_isolation "
                + "ON societix_scheduled_job_it_work");
        ownerJdbc.execute("ALTER TABLE societix_scheduled_job_it_work ENABLE ROW LEVEL SECURITY");
        ownerJdbc.execute("ALTER TABLE societix_scheduled_job_it_work FORCE ROW LEVEL SECURITY");
        ownerJdbc.execute("""
                CREATE POLICY societix_scheduled_job_it_work_tenant_isolation
                ON societix_scheduled_job_it_work
                USING (tenant_id = CASE
                    WHEN current_setting('app.tenant_id', true) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                    THEN current_setting('app.tenant_id', true)::uuid
                    ELSE NULL::uuid
                END)
                WITH CHECK (tenant_id = CASE
                    WHEN current_setting('app.tenant_id', true) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                    THEN current_setting('app.tenant_id', true)::uuid
                    ELSE NULL::uuid
                END)
                """);
        ownerJdbc.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON societix_scheduled_job_it_work TO societix_app");
        return new ScheduledJobsTestTable();
    }

    @Bean
    @DependsOn("scheduledJobsTestTable")
    DueWorkJob treasuryDueWorkJob(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        return new JdbcDueWorkJob("treasury", jdbcTemplate, new TransactionTemplate(transactionManager));
    }

    @Bean
    @DependsOn("scheduledJobsTestTable")
    DueWorkJob privacyDueWorkJob(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        return new JdbcDueWorkJob("privacy", jdbcTemplate, new TransactionTemplate(transactionManager));
    }

    static final class ScheduledJobsTestTable {}

    static class JdbcDueWorkJob implements DueWorkJob {

        private final String jobKey;
        private final JdbcTemplate jdbcTemplate;
        private final TransactionTemplate transactionTemplate;

        JdbcDueWorkJob(String jobKey, JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
            this.jobKey = jobKey;
            this.jdbcTemplate = jdbcTemplate;
            this.transactionTemplate = transactionTemplate;
        }

        @Override
        public String jobKey() {
            return jobKey;
        }

        @Override
        public void runDueWork() {
            for (UUID tenantId : TENANTS) {
                transactionTemplate.executeWithoutResult(status -> {
                    jdbcTemplate.queryForObject(
                            "SELECT set_config('app.tenant_id', ?, true)", String.class, tenantId.toString());
                    jdbcTemplate.update(
                            """
                            UPDATE societix_scheduled_job_it_work
                            SET processed_at = now()
                            WHERE tenant_id = ? AND job_key = ? AND due_at <= now() AND processed_at IS NULL
                            """,
                            tenantId,
                            jobKey);
                });
            }
        }
    }
}
