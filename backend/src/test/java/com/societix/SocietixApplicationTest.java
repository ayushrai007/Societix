package com.societix;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class SocietixApplicationTest {

    private static final UUID PLATFORM_ADMIN_ID = UUID.fromString("8db74fbb-b015-40d4-818c-0f4ab3524e1b");
    private static final String PLATFORM_ADMIN_UID = "societix-it-platform-admin";

    private static final Logger LOGGER = LoggerFactory.getLogger(SocietixApplicationTest.class);

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    SocietixApplicationTest(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void ensureCurrentPlatformAdminExists() {
        jdbcTemplate.update(
                "INSERT INTO identity_account (id, firebase_uid, phone_number_cipher) VALUES (?, ?, decode('00', 'hex')) "
                        + "ON CONFLICT (id) DO NOTHING",
                PLATFORM_ADMIN_ID,
                PLATFORM_ADMIN_UID);
        jdbcTemplate.update(
                "INSERT INTO platform_admin_account (identity_id, status, authorization_version) VALUES (?, 'ACTIVE', 1) "
                        + "ON CONFLICT (identity_id) DO UPDATE SET status = 'ACTIVE', authorization_version = 1",
                PLATFORM_ADMIN_ID);
    }

    @Test
    void applicationStarts() {
        // A successful Spring context load proves the application can start.
    }

    @Test
    void actuatorHealthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorMetricsEndpointExposesHttpRequestMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .with(platformAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").value(Matchers.hasItem("http.server.requests")));
    }

    @Test
    void applicationLogsAreStructuredJson(CapturedOutput output) throws Exception {
        LOGGER.atInfo()
                .addKeyValue("societix_test_marker", "soc41-structured-log")
                .addKeyValue("tenantId", "soc41-test-tenant")
                .addKeyValue("traceId", "soc41-test-trace")
                .log("SOC-41 structured logging verification");

        String logLine = output.getOut().lines()
                .filter(line -> line.contains("\"societix_test_marker\":\"soc41-structured-log\""))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Structured log entry was not captured"));
        JsonNode logEvent = objectMapper.readTree(logLine);

        Assertions.assertThat(logEvent.path("message").asText())
                .isEqualTo("SOC-41 structured logging verification");
    }

    @Test
    void httpClientErrorIsVisibleInMetricsWithoutLoggingPersonalData(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/v1/societies")
                        .with(platformAdminToken())
                        .contentType("application/json")
                        .content("""
                                {"name":"Test 9876543210","address":"Test address","state":"invalid"}
                                """))
                .andExpect(status().isBadRequest());

        String metricsResponse = mockMvc.perform(get("/actuator/metrics/http.server.requests")
                        .with(platformAdminToken())
                        .queryParam("tag", "status:400"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode metrics = objectMapper.readTree(metricsResponse);
        double errorCount = 0;
        for (JsonNode measurement : metrics.path("measurements")) {
            if ("COUNT".equals(measurement.path("statistic").asText())) {
                errorCount = measurement.path("value").asDouble();
                break;
            }
        }

        Assertions.assertThat(errorCount).isPositive();
        Assertions.assertThat(output.getOut()).doesNotContain("9876543210");
        Assertions.assertThat(output.getErr()).doesNotContain("9876543210");
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor platformAdminToken() {
        return jwt().jwt(token -> token
                        .subject(PLATFORM_ADMIN_UID)
                        .claim("userId", PLATFORM_ADMIN_ID.toString())
                        .claim("authorizationVersion", 1)
                        .claim("roles", List.of("PLATFORM_ADMIN")))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_PLATFORM_ADMIN"));
    }
}
