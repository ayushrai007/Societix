package com.societix;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * SOC-100: V7 must deploy under a non-superuser, non-BYPASSRLS, database-owner migration role and
 * back-fill the statutory funds of an already existing society through its SECURITY DEFINER helper.
 */
@Testcontainers
class LedgerMigrationRoleIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new TestcontainersConfiguration().postgresContainer();
    static final String DEPLOYER = "societix_deployer";

    @Test
    void nonSuperuserMigrationRoleBackfillsExistingSocietyAndDropsHelper() throws Exception {
        flyway(POSTGRES.getUsername(), POSTGRES.getPassword(), "6").migrate();
        String society = "11111111-1111-1111-1111-111111111111";
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var s = c.createStatement()) {
            s.execute("CREATE ROLE " + DEPLOYER + " LOGIN PASSWORD 'deployer' NOSUPERUSER NOBYPASSRLS");
            s.execute("GRANT societix_outbox_queue_reader TO " + DEPLOYER);
            s.execute("DO $$ BEGIN EXECUTE format('ALTER DATABASE %I OWNER TO " + DEPLOYER + "', current_database()); END $$");
            s.execute("DO $$ DECLARE r record; BEGIN "
                    + "FOR r IN SELECT c.oid::regclass AS n, c.relkind FROM pg_class c JOIN pg_namespace s ON s.oid = c.relnamespace "
                    + "WHERE s.nspname = 'public' AND c.relkind IN ('r','S','p') LOOP "
                    + "EXECUTE format('ALTER %s %s OWNER TO " + DEPLOYER + "', CASE r.relkind WHEN 'S' THEN 'SEQUENCE' ELSE 'TABLE' END, r.n); END LOOP; "
                    + "FOR r IN SELECT p.oid::regprocedure AS n FROM pg_proc p JOIN pg_namespace s ON s.oid = p.pronamespace "
                    + "WHERE s.nspname = 'public' AND p.proowner = (SELECT oid FROM pg_roles WHERE rolname = current_user) LOOP "
                    + "EXECUTE format('ALTER FUNCTION %s OWNER TO " + DEPLOYER + "', r.n); END LOOP; END $$");
            s.execute("INSERT INTO society(id,tenant_id,name,address,state) VALUES ('" + society + "','" + society
                    + "','Existing','Test address','MH')");
        }

        flyway(DEPLOYER, "deployer", "latest").migrate();

        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var s = c.createStatement()) {
            assertThat(scalar(s, "select rolsuper or rolbypassrls from pg_roles where rolname = '" + DEPLOYER + "'")).isEqualTo(false);
            assertThat(scalar(s, "select count(*) from ledger_account where tenant_id = '" + society
                    + "' and is_fund and code in ('3000','3100','3200','3300')")).isEqualTo(4L);
            assertThat(scalar(s, "select to_regprocedure('backfill_ledger_funds()') is null")).isEqualTo(true);
            assertThat(scalar(s, "select not has_table_privilege('societix_outbox_queue_reader', 'ledger_account', 'SELECT')"
                    + " and not has_table_privilege('societix_outbox_queue_reader', 'society', 'SELECT')"
                    + " and not has_schema_privilege('societix_outbox_queue_reader', 'public', 'CREATE')")).isEqualTo(true);
        }
    }

    private static Object scalar(java.sql.Statement s, String sql) throws Exception {
        try (var r = s.executeQuery(sql)) {
            assertThat(r.next()).isTrue();
            return r.getObject(1);
        }
    }

    private static Flyway flyway(String user, String password, String target) {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), user, password)
                .placeholders(Map.of("app_runtime_role", TestcontainersConfiguration.APP_RUNTIME_ROLE))
                .target(target).load();
    }
}
