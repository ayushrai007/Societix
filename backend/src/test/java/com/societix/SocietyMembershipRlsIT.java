package com.societix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SocietyMembershipRlsIT {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    SocietyMembershipRlsIT(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void membershipsAreIsolatedAndMissingOrInvalidTenantContextFailsClosed() throws Exception {
        UUID societyA = UUID.randomUUID();
        UUID societyB = UUID.randomUUID();
        UUID identityA = UUID.randomUUID();
        UUID identityB = UUID.randomUUID();
        UUID membershipA = UUID.randomUUID();
        UUID membershipB = UUID.randomUUID();
        createSociety(societyA, "Aster Court");
        createSociety(societyB, "Birch Court");
        createIdentity(identityA, "rls-user-a");
        createIdentity(identityB, "rls-user-b");
        createMembership(societyA, membershipA, identityA);
        createMembership(societyB, membershipB, identityB);

        assertThat(countAsTenant(societyA, membershipA)).isEqualTo(1);
        assertThat(countAsTenant(societyA, membershipB)).isZero();
        assertThat(updateAsTenant(societyA, membershipB)).isZero();
        assertThat(countAsTenant(societyB, membershipB)).isEqualTo(1);
        assertThat(countWithTenant(null)).isZero();
        assertThat(countWithTenant("not-a-uuid")).isZero();

        UUID crossTenantMembership = UUID.randomUUID();
        UUID crossTenantIdentity = UUID.randomUUID();
        createIdentity(crossTenantIdentity, "rls-user-cross-write");
        assertThatThrownBy(() -> insertMembershipUnderTenant(
                        societyA, societyB, crossTenantMembership, crossTenantIdentity))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("row-level security policy");
    }

    private void createSociety(UUID id, String name) throws SQLException {
        inTransaction(id, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into society (id, tenant_id, name, address, state) values (?, ?, ?, ?, 'MH')")) {
                statement.setObject(1, id);
                statement.setObject(2, id);
                statement.setString(3, name);
                statement.setString(4, "Test Road");
                statement.executeUpdate();
            }
        });
    }

    private void createIdentity(UUID id, String firebaseUid) {
        jdbcTemplate.update(
                "insert into identity_account (id, firebase_uid, phone_number_cipher) values (?, ?, ?)",
                id,
                firebaseUid,
                new byte[] {1});
    }

    private void createMembership(UUID tenantId, UUID membershipId, UUID identityId) throws SQLException {
        insertMembershipUnderTenant(tenantId, tenantId, membershipId, identityId);
    }

    private void insertMembershipUnderTenant(UUID contextTenant, UUID rowTenant, UUID membershipId, UUID identityId)
            throws SQLException {
        inTransaction(contextTenant, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into society_membership (id, tenant_id, identity_id, membership_type) values (?, ?, ?, 'RESIDENT')")) {
                statement.setObject(1, membershipId);
                statement.setObject(2, rowTenant);
                statement.setObject(3, identityId);
                statement.executeUpdate();
            }
        });
    }

    private int countAsTenant(UUID tenantId, UUID membershipId) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                setTenant(statement, tenantId.toString());
                try (var result = statement.executeQuery(
                        "select count(*) from society_membership where id = '" + membershipId + "'")) {
                    result.next();
                    return result.getInt(1);
                }
            } finally {
                connection.rollback();
            }
        }
    }

    private int countWithTenant(String tenantId) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                setTenant(statement, tenantId == null ? "" : tenantId);
                try (var result = statement.executeQuery("select count(*) from society_membership")) {
                    result.next();
                    return result.getInt(1);
                }
            } finally {
                connection.rollback();
            }
        }
    }

    private int updateAsTenant(UUID contextTenant, UUID membershipId) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                setTenant(statement, contextTenant.toString());
                return statement.executeUpdate(
                        "update society_membership set status = 'SUSPENDED' where id = '" + membershipId + "'");
            } finally {
                connection.rollback();
            }
        }
    }

    private void inTransaction(UUID tenantId, SqlWork work) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                setTenant(statement, tenantId.toString());
                work.run(connection);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void setTenant(Statement statement, String tenantId) throws SQLException {
        try (var ignored = statement.executeQuery("select set_config('app.tenant_id', '" + tenantId + "', true)")) {
            ignored.next();
        }
    }

    @FunctionalInterface
    private interface SqlWork {
        void run(Connection connection) throws SQLException;
    }
}
