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
 * </ul>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FlywayMigrationTest {

    private final Flyway flyway;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    FlywayMigrationTest(Flyway flyway, JdbcTemplate jdbcTemplate) {
        this.flyway = flyway;
        this.jdbcTemplate = jdbcTemplate;
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
}
