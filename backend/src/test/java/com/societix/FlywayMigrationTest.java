package com.societix;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SOC-5 acceptance tests, run against a real PostgreSQL container.
 *
 * <ul>
 *   <li>"Flyway runs migrations on app startup" — {@link #flywayRunsMigrationsOnApplicationStartup()}</li>
 *   <li>"a sample V1 migration applies cleanly" — {@link #baselineV1MigrationAppliesCleanly()}</li>
 *   <li>"the Society table and RLS policy are stored by Flyway" — {@link #societyMigrationAppliesCleanly()}</li>
 *   <li>"the outbox table and its RLS policy are stored by Flyway" — {@link #outboxMigrationAppliesCleanly()}</li>
 *   <li>"outbox dispatch infrastructure is migrated" — {@link #outboxDispatchMigrationAppliesCleanly()}</li>

 * </ul>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FlywayMigrationTest {

    private final Flyway flyway;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    FlywayMigrationTest(Flyway flyway) {
        this.flyway = flyway;
        // Migration metadata is read as the owner role Flyway migrates with: the application's
        // runtime role has no access to flyway_schema_history.
        this.jdbcTemplate = new JdbcTemplate(flyway.getConfiguration().getDataSource());

    }

    @Test
    void flywayRunsMigrationsOnApplicationStartup() {
        // Nothing in this test calls migrate(): the history table exists only because
        // Flyway ran while the application context started.
        Integer historyTables = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'flyway_schema_history'",
                Integer.class);
        assertThat(historyTables).isEqualTo(1);

        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current()).isNotNull();
    }

    @Test
    void baselineV1MigrationAppliesCleanly() {
        List<MigrationInfo> applied = Arrays.asList(flyway.info().applied());
        assertThat(applied)
                .filteredOn(migration -> migration.getVersion() != null
                        && migration.getVersion().getVersion().equals("1"))
                .singleElement()
                .satisfies(migration -> {
                    assertThat(migration.getVersion().getVersion()).isEqualTo("1");
                    assertThat(migration.getDescription()).isEqualTo("baseline");
                    assertThat(migration.getState()).isEqualTo(MigrationState.SUCCESS);
                });

        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select version, script, success from flyway_schema_history where version is not null");
        assertThat(history)
                .filteredOn(row -> "1".equals(row.get("version")))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row).containsEntry("version", "1");
                    assertThat(row).containsEntry("script", "V1__baseline.sql");
                    assertThat(row).containsEntry("success", true);
                });

        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    void societyMigrationAppliesCleanly() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select version, script, success from flyway_schema_history where version = '2'");
        assertThat(history).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("version", "2");
            assertThat(row).containsEntry("script", "V2__create_society.sql");
            assertThat(row).containsEntry("success", true);
        });
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from information_schema.columns where table_name = 'society' and column_name in ('id', 'tenant_id', 'name', 'address', 'state')",
                        Integer.class))
                .isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from pg_policies where tablename = 'society' and policyname = 'society_tenant_isolation'",
                        Integer.class))
                .isEqualTo(1);
    }

    @Test
    void outboxMigrationAppliesCleanly() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select version, script, success from flyway_schema_history where version = '4'");
        assertThat(history).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("version", "4");
            assertThat(row).containsEntry("script", "V4__create_outbox.sql");
            assertThat(row).containsEntry("success", true);
        });
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from information_schema.columns where table_name = 'outbox' "
                                + "and column_name in ('id', 'tenant_id', 'event_type', 'payload', 'created_at', 'processed_at', 'attempts')",
                        Integer.class))
                .isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from pg_policies where tablename = 'outbox' "
                                + "and policyname = 'outbox_tenant_isolation'",
                        Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "select relrowsecurity and relforcerowsecurity from pg_class where oid = 'outbox'::regclass",
                        Boolean.class))
                .isTrue();
    }

    @Test
    void outboxDispatchMigrationAppliesCleanly() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select version, script, success from flyway_schema_history where version = '5'");
        assertThat(history).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("version", "5");
            assertThat(row).containsEntry("script", "V5__configure_outbox_dispatch.sql");
            assertThat(row).containsEntry("success", true);
        });
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from information_schema.columns where table_name = 'outbox' "
                                + "and column_name = 'next_attempt_at'",
                        Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "select relrowsecurity and relforcerowsecurity from pg_class "
                                + "where oid = 'outbox_handler_receipt'::regclass",
                        Boolean.class))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from pg_policies where tablename = 'outbox_handler_receipt' "
                                + "and policyname = 'outbox_handler_receipt_tenant_isolation'",
                        Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from information_schema.tables where table_name = 'shedlock'",
                        Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from pg_proc where proname = 'outbox_pending_tenants'",
                        Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "select has_function_privilege('societix_app', 'outbox_pending_tenants(integer)', 'EXECUTE')",
                        Boolean.class))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                        "select has_table_privilege('societix_outbox_queue_reader', 'outbox', 'SELECT') "
                                + "and not has_table_privilege('societix_outbox_queue_reader', 'society', 'SELECT')",
                        Boolean.class))
                .isTrue();
    }

}
