package com.societix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** SOC-100 executable acceptance tests; all ledger operations use SOC-10's runtime login. */
@Testcontainers
class LedgerIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new TestcontainersConfiguration().postgresContainer();
    static final String[] TABLES = {"accounting_period", "ledger_account", "ledger_transaction", "ledger_entry"};
    static UUID existingSociety;

    @BeforeAll
    static void migrateExistingSociety() throws Exception {
        migrations("3").migrate();
        existingSociety = society();
        migrations("latest").migrate();
    }

    private static Flyway migrations(String target) {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .placeholders(Map.of("app_runtime_role", TestcontainersConfiguration.APP_RUNTIME_ROLE))
                .target(target).load();
    }

    @Test
    void migrationCreatesLedgerTablesWithForcedRlsAndRestrictedRuntimeRole() throws Exception {
        assertThat(migrations("latest").validateWithResult().validationSuccessful).isTrue();
        tenant(existingSociety, c -> {
            assertThat(value(c, "select current_user")).isEqualTo(TestcontainersConfiguration.APP_RUNTIME_ROLE);
            assertThat(value(c, "select rolsuper or rolbypassrls from pg_roles where rolname = current_user")).isEqualTo(false);
            for (String table : TABLES) {
                assertThat(value(c, "select relrowsecurity and relforcerowsecurity and relowner <> "
                        + "(select oid from pg_roles where rolname = current_user) from pg_class where oid = ?::regclass", table))
                        .isEqualTo(true);
                value(c, "select count(*) from " + table);
            }
            assertThat(value(c, "select to_regprocedure('backfill_ledger_funds()') is null")).isEqualTo(true);
            assertThat(value(c, "select not has_table_privilege('societix_outbox_queue_reader', 'society', 'SELECT') "
                    + "and not has_table_privilege('societix_outbox_queue_reader', 'ledger_account', 'INSERT') "
                    + "and not has_sequence_privilege('societix_outbox_queue_reader', 'ledger_account_id_seq', 'USAGE')"))
                    .isEqualTo(true);
        });
    }

    @Test
    void standardFundsAreSeededForExistingAndNewSocietiesIdempotently() throws Exception {
        for (UUID id : new UUID[] {existingSociety, society()}) {
            tenant(id, c -> {
                execute(c, "select seed_ledger_funds(?)", id);
                assertThat(value(c, "select string_agg(code || ':' || name, ',' order by code) from ledger_account"))
                        .isEqualTo("3000:Common Maintenance Fund,3100:Sinking Fund,3200:Repair & Maintenance Fund,3300:Reserve Fund");
                assertThat(value(c, "select count(*) from ledger_account where type='EQUITY' and normal_side='C' and is_fund and active"))
                        .isEqualTo(4L);
            });
        }
    }

    @Test
    void balancedPostingCommitsAndDuplicateEventIsRejected() throws Exception {
        Fixture f = fixture();
        post(f, 100, 100);
        tenant(f.tenant(), c -> assertThat(value(c, "select sum(debit_minor) = sum(credit_minor) from ledger_entry")).isEqualTo(true));
        Fixture duplicate = new Fixture(f.tenant(), f.period(), f.debitAccount(), f.creditAccount(), UUID.randomUUID());
        rejects("23505", () -> post(duplicate, 100, 100));
        tenant(f.tenant(), c -> assertThat(value(c, "select count(*) from ledger_transaction")).isEqualTo(1L));
    }

    @Test
    void unbalancedAndEmptyPostingsFailAtCommitAndRollBack() throws Exception {
        Fixture f = fixture();
        rejects("23514", () -> post(f, 100, 99));
        rejects("23514", () -> tenant(f.tenant(), c -> header(c, f, f.period(), null)));
        tenant(f.tenant(), c -> {
            assertThat(value(c, "select count(*) from ledger_transaction")).isEqualTo(0L);
            assertThat(value(c, "select count(*) from ledger_entry")).isEqualTo(0L);
        });
    }

    @Test
    void invalidLinesAndLaterUnbalancedInsertsAreRejected() throws Exception {
        Fixture f = fixture();
        post(f, 100, 100);
        for (long[] amounts : new long[][] {{-1, 0}, {0, -1}, {0, 0}, {1, 1}, {1, 0}}) {
            rejects("23514", () -> tenant(f.tenant(), c -> entry(c, f, f.journal(), f.debitAccount(), amounts[0], amounts[1])));
        }
        tenant(f.tenant(), c -> assertThat(value(c, "select count(*) from ledger_entry")).isEqualTo(2L));
    }

    @Test
    void postedHeadersAndEntriesCannotBeUpdatedOrDeleted() throws Exception {
        Fixture f = fixture();
        post(f, 100, 100);
        for (String table : new String[] {"ledger_transaction", "ledger_entry"}) {
            rejects("42501", () -> tenant(f.tenant(), c -> execute(c, "update " + table + " set tenant_id = tenant_id")));
            rejects("42501", () -> tenant(f.tenant(), c -> execute(c, "delete from " + table)));
        }
    }

    @Test
    void societiesCannotReadOrWriteEachOthersLedgerAndMissingContextFailsClosed() throws Exception {
        Fixture a = fixture(), b = fixture();
        post(a, 100, 100);
        post(b, 200, 200); // The same idempotency key is allowed in different societies.
        for (String table : TABLES) {
            for (Fixture own : new Fixture[] {a, b}) {
                UUID other = own == a ? b.tenant() : a.tenant();
                tenant(own.tenant(), c -> {
                    assertThat((Long) value(c, "select count(*) from " + table)).isPositive();
                    assertThat(value(c, "select count(*) from " + table + " where tenant_id = ?", other)).isEqualTo(0L);
                });
            }
            for (Object context : new Object[] {null, "", "invalid-uuid"}) {
                tenant(context, c -> assertThat(value(c, "select count(*) from " + table)).isEqualTo(0L));
            }
            // Clone B's complete row under A's context: RLS must reject it before uniqueness checks.
            rejects("42501", () -> tenant(a.tenant(), c -> execute(c,
                    "insert into " + table + " overriding system value select * from json_populate_record(null::" + table + ", ?::json)",
                    row(b.tenant(), table))));
        }
        rejects("23503", () -> tenant(a.tenant(), c -> entry(c, a, a.journal(), b.debitAccount(), 1, 0)));
        rejects("23503", () -> tenant(a.tenant(), c -> entry(c, a, b.journal(), a.debitAccount(), 1, 0)));
        Fixture fresh = fixture();
        rejects("23503", () -> tenant(fresh.tenant(), c -> header(c, fresh, b.period(), null)));
        rejects("23503", () -> tenant(fresh.tenant(), c -> header(c, fresh, fresh.period(), b.journal())));
        rejects("23503", () -> tenant(a.tenant(), c -> execute(c,
                "insert into ledger_account(tenant_id,code,name,type,normal_side,parent_id) values (?,'9999','test','EQUITY','C',?)",
                a.tenant(), b.debitAccount())));
    }

    private static String row(UUID id, String table) throws Exception {
        String[] result = new String[1];
        tenant(id, c -> result[0] = (String) value(c, "select row_to_json(t)::text from " + table + " t limit 1"));
        return result[0];
    }

    private static UUID society() throws Exception {
        UUID id = UUID.randomUUID();
        tenant(id, c -> execute(c, "insert into society(id,tenant_id,name,address,state) values (?,?,'Ledger fixture','Test address','MH')", id, id));
        return id;
    }

    private static Fixture fixture() throws Exception {
        UUID id = society();
        long[] ids = new long[3];
        tenant(id, c -> {
            ids[0] = (Long) value(c, "insert into accounting_period(tenant_id,starts_on,ends_on) values (?,'2026-04-01','2027-03-31') returning id", id);
            ids[1] = (Long) value(c, "select id from ledger_account where code='3000'");
            ids[2] = (Long) value(c, "select id from ledger_account where code='3100'");
        });
        return new Fixture(id, ids[0], ids[1], ids[2], UUID.randomUUID());
    }

    private static void header(Connection c, Fixture f, long period, UUID reverses) throws SQLException {
        execute(c, "insert into ledger_transaction(id,tenant_id,txn_date,description,source_type,source_ref,idempotency_key,period_id,posted_by,reverses_txn) "
                + "values (?,?,'2026-11-18','Fund transfer','TRANSFER','fixture','event:1',?,'test',?)", f.journal(), f.tenant(), period, reverses);
    }

    private static void entry(Connection c, Fixture f, UUID journal, long account, long debit, long credit) throws SQLException {
        execute(c, "insert into ledger_entry(tenant_id,txn_id,account_id,debit_minor,credit_minor) values (?,?,?,?,?)",
                f.tenant(), journal, account, debit, credit);
    }

    private static void post(Fixture f, long debit, long credit) throws Exception {
        tenant(f.tenant(), c -> {
            header(c, f, f.period(), null);
            entry(c, f, f.journal(), f.debitAccount(), debit, 0);
            entry(c, f, f.journal(), f.creditAccount(), 0, credit);
        });
    }

    private static void tenant(Object id, Work work) throws Exception {
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), TestcontainersConfiguration.APP_RUNTIME_ROLE,
                POSTGRES.getEnvMap().get("APP_DB_PASSWORD"))) {
            c.setAutoCommit(false);
            try {
                // SOC-10's set_config(..., true) is SET LOCAL: context expires on commit/rollback.
                if (id != null) execute(c, "select set_config('app.tenant_id', ?, true)", id.toString());
                work.run(c);
                c.commit();
            } finally {
                c.rollback();
            }
        }
    }

    private static Object value(Connection c, String sql, Object... args) throws SQLException {
        try (var statement = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getObject(1);
            }
        }
    }

    private static void execute(Connection c, String sql, Object... args) throws SQLException {
        try (var statement = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            statement.execute();
        }
    }

    private static void rejects(String state, org.assertj.core.api.ThrowableAssert.ThrowingCallable work) {
        assertThatThrownBy(work).isInstanceOf(SQLException.class)
                .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo(state));
    }
    @FunctionalInterface
    private interface Work { void run(Connection connection) throws Exception; }
    private record Fixture(UUID tenant, long period, long debitAccount, long creditAccount, UUID journal) {}
}
