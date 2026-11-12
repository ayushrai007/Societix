package com.societix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.TestInstance;

/**
 * SOC-10 acceptance tests against the real PostgreSQL Testcontainer.
 *
 * <ul>
 *   <li>"can create a society via an admin endpoint" — {@link #adminCanCreateSocietyViaEndpoint()}</li>
 *   <li>"society has id, name, address, state" — {@link #createdSocietyContainsIdNameAddressAndState()}</li>
 *   <li>"invalid input uses problem+json" — {@link #invalidSocietyRequestReturnsProblemJson()}</li>
 *   <li>"society A cannot read society B" — {@link #societyRowsAreIsolatedByTenantRls()}</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SocietyOnboardingIT {

    private static final String TEST_ROLE = "societix_rls_test";

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Autowired
    SocietyOnboardingIT(MockMvc mockMvc, JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @BeforeAll
    void grantRlsTestRole() {
        jdbcTemplate.execute("create role " + TEST_ROLE + " nologin nosuperuser nobypassrls");
        jdbcTemplate.execute("grant usage on schema public to " + TEST_ROLE);
        jdbcTemplate.execute("grant select on table society to " + TEST_ROLE);
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
        assertThat(jdbcTemplate.queryForObject("select id from society where id = ?", UUID.class, id)).isEqualTo(id);
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

        Integer visibleFromA = withRlsRoleAndTenant(societyA, "select count(*) from society");
        Integer otherTenantFromA = withRlsRoleAndTenant(societyA, "select count(*) from society where id = '" + societyB + "'");
        Integer visibleWithoutTenant = withRlsRoleAndTenant(null, "select count(*) from society");
        Integer visibleWithInvalidTenant = withRlsRoleAndTenant("not-a-uuid", "select count(*) from society");

        assertThat(visibleFromA).isEqualTo(1);
        assertThat(otherTenantFromA).isZero();
        assertThat(visibleWithoutTenant).isZero();
        assertThat(visibleWithInvalidTenant).isZero();
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

    private Integer withRlsRoleAndTenant(Object tenantId, String query) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("set local role " + TEST_ROLE);
            if (tenantId == null) {
                statement.execute("select set_config('app.tenant_id', '', true)");
            } else {
                statement.execute("select set_config('app.tenant_id', '" + tenantId + "', true)");
            }
            try (var result = statement.executeQuery(query)) {
                result.next();
                int count = result.getInt(1);
                connection.rollback();
                return count;
            }
        }
    }

}
