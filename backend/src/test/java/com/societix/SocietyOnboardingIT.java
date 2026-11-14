package com.societix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.societix.platform.society.data.SocietyRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SOC-10 acceptance tests against the real PostgreSQL Testcontainer.
 *
 * <p>Every database check here goes through the application's own datasource, which connects as
 * the runtime role (not a superuser, no BYPASSRLS, not the table owner). No test switches role,
 * so the isolation they prove is the isolation the running application gets.
 *
 * <ul>
 *   <li>"can create a society via an admin endpoint" — {@link #adminCanCreateSocietyViaEndpoint()}</li>
 *   <li>"society has id, name, address, state" — {@link #createdSocietyContainsIdNameAddressAndState()}</li>
 *   <li>"invalid input uses problem+json" — {@link #invalidSocietyRequestReturnsProblemJson()}</li>
 *   <li>"society A cannot read society B" — {@link #societyRowsAreIsolatedByTenantRls()}</li>
 *   <li>"society A cannot write society B" — {@link #societyCannotWriteAnotherTenantsRow()}</li>
 *   <li>"a query without tenant context fails closed for the application's runtime role" —
 *       {@link #untenantedQueryFailsClosedForTheApplicationRuntimeRole()}</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SocietyOnboardingIT {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final SocietyRepository societies;

    @Autowired
    SocietyOnboardingIT(
            MockMvc mockMvc, JdbcTemplate jdbcTemplate, DataSource dataSource, SocietyRepository societies) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.societies = societies;
    }

    @Test
    void adminCanCreateSocietyViaEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/societies")
                        .header("X-Admin-Role", "PLATFORM_ADMIN")
                        .contentType("application/json")
                        .content("""
                                {"name":"Maple Residency","address":"12 MG Road, Pune","state":"MH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/societies/[0-9a-f-]+")));
    }

    @Test
    void createdSocietyContainsIdNameAddressAndState() throws Exception {
        String response = mockMvc.perform(post("/api/v1/societies")
                        .header("X-Admin-Role", "PLATFORM_ADMIN")
                        .contentType("application/json")
                        .content("""
                                {"name":"Lakeview Heights","address":"88 Residency Road, Bengaluru","state":"KA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("Lakeview Heights"))
                .andExpect(jsonPath("$.address").value("88 Residency Road, Bengaluru"))
                .andExpect(jsonPath("$.state").value("KA"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID id = responseId(response);
        assertThat(countAsTenant(
                        id,
                        "select count(*) from society where id = '" + id
                                + "' and name = 'Lakeview Heights' and address = '88 Residency Road, Bengaluru'"
                                + " and state = 'KA'"))
                .isEqualTo(1);
    }

    @Test
    void invalidSocietyRequestReturnsProblemJson() throws Exception {
        mockMvc.perform(post("/api/v1/societies")
                        .header("X-Admin-Role", "PLATFORM_ADMIN")
                        .contentType("application/json")
                        .content("""
                                {"name":"","address":"","state":"Maharashtra"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://errors.societix.dev/validation"))
                .andExpect(jsonPath("$.code").value("SOC-400-VALIDATION"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void societyRowsAreIsolatedByTenantRls() throws Exception {
        UUID societyA = createSociety("Aster Court", "1 A Road, Mumbai", "MH");
        UUID societyB = createSociety("Birch Court", "2 B Road, Mumbai", "MH");

        Integer visibleFromA = countAsTenant(societyA, "select count(*) from society");
        Integer otherTenantFromA = countAsTenant(societyA, "select count(*) from society where id = '" + societyB + "'");
        Integer visibleWithoutTenant = countAsTenant(null, "select count(*) from society");
        Integer visibleWithInvalidTenant = countAsTenant("not-a-uuid", "select count(*) from society");

        assertThat(visibleFromA).isEqualTo(1);
        assertThat(otherTenantFromA).isZero();
        assertThat(visibleWithoutTenant).isZero();
        assertThat(visibleWithInvalidTenant).isZero();
    }

    @Test
    void societyCannotWriteAnotherTenantsRow() throws Exception {
        UUID societyA = createSociety("Cedar Court", "3 C Road, Mumbai", "MH");
        UUID societyB = UUID.randomUUID();

        assertThatThrownBy(() -> executeAsTenant(
                        societyA,
                        "insert into society (id, tenant_id, name, address, state) values ('"
                                + societyB
                                + "', '"
                                + societyB
                                + "', 'Dahlia Court', '4 D Road, Mumbai', 'MH')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("row-level security policy");

        // Tenant B's own context would see the row if the insert had been persisted.
        assertThat(countAsTenant(societyB, "select count(*) from society where id = '" + societyB + "'"))
                .isZero();
    }

    @Test
    void untenantedQueryFailsClosedForTheApplicationRuntimeRole() throws Exception {
        UUID societyA = createSociety("Elm Court", "5 E Road, Mumbai", "MH");
        UUID societyB = createSociety("Fir Court", "6 F Road, Mumbai", "MH");

        // The application's datasource really is the restricted runtime role, so RLS applies to it.
        Map<String, Object> role = jdbcTemplate.queryForMap("""
                select current_user as name, r.rolsuper, r.rolbypassrls,
                       (select tableowner from pg_tables where tablename = 'society') as society_owner
                from pg_roles r
                where r.rolname = current_user
                """);
        assertThat(role).containsEntry("name", TestcontainersConfiguration.APP_RUNTIME_ROLE);
        assertThat(role).containsEntry("rolsuper", false);
        assertThat(role).containsEntry("rolbypassrls", false);
        assertThat(role.get("society_owner")).isNotEqualTo(TestcontainersConfiguration.APP_RUNTIME_ROLE);

        // Both rows exist and each is visible to its own tenant ...
        assertThat(countAsTenant(societyA, "select count(*) from society where id = '" + societyA + "'"))
                .isEqualTo(1);
        assertThat(countAsTenant(societyB, "select count(*) from society where id = '" + societyB + "'"))
                .isEqualTo(1);

        // ... but the application's queries with no tenant context see nothing at all.
        assertThat(jdbcTemplate.queryForObject("select count(*) from society", Integer.class)).isZero();
        assertThat(societies.findAll()).isEmpty();
        assertThat(societies.findById(societyA)).isEmpty();
        assertThat(societies.count()).isZero();
    }

    private UUID createSociety(String name, String address, String state) throws Exception {
        String response = mockMvc.perform(post("/api/v1/societies")
                        .header("X-Admin-Role", "PLATFORM_ADMIN")
                        .contentType("application/json")
                        .content("{\"name\":\"" + name + "\",\"address\":\"" + address + "\",\"state\":\"" + state + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return responseId(response);
    }

    private UUID responseId(String response) {
        return UUID.fromString(response.replaceFirst(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1"));
    }

    /** Runs a count query on an application connection, inside a transaction scoped to {@code tenantId}. */
    private Integer countAsTenant(Object tenantId, String query) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                setTenant(statement, tenantId);
                try (var result = statement.executeQuery(query)) {
                    result.next();
                    return result.getInt(1);
                }
            } finally {
                connection.rollback();
            }
        }
    }

    /** Runs a write on an application connection, inside a transaction scoped to {@code tenantId}. */
    private void executeAsTenant(UUID tenantId, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                setTenant(statement, tenantId);
                statement.executeUpdate(sql);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void setTenant(Statement statement, Object tenantId) throws SQLException {
        String value = tenantId == null ? "" : tenantId.toString();
        statement.execute("select set_config('app.tenant_id', '" + value + "', true)");
    }
}
