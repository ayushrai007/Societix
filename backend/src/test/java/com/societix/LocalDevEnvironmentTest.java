package com.societix;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Starts the repository's docker-compose.yml (SOC-3) and points the application at it.
 *
 * <p>Host ports are set to 0 so Docker picks free ones, and Testcontainers uses its own project
 * name, so this runs safely next to a developer's own {@code docker compose up} stack.
 */
@Testcontainers
@SpringBootTest
class LocalDevEnvironmentTest {

    private static final Map<String, String> EXPECTED_IMAGES = Map.of(
            "postgres", "pgvector/pgvector:0.8.7-pg18",
            "redis", "redis:8.2-alpine",
            "pa-sandbox", "mockserver/mockserver:9.2.0");

    @Container
    static final ComposeContainer COMPOSE = new ComposeContainer(new File("../docker-compose.yml"))
            .withEnv(Map.of("DB_PORT", "0", "REDIS_PORT", "0", "PA_MOCK_PORT", "0"))
            .waitingFor("postgres", Wait.forHealthcheck())
            .waitingFor("redis", Wait.forHealthcheck())
            .waitingFor("pa-sandbox", Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(2)));

    @DynamicPropertySource
    static void useComposeDatabase(DynamicPropertyRegistry registry) {
        ContainerState postgres = service("postgres");
        registry.add("DB_HOST", postgres::getHost);
        registry.add("DB_PORT", () -> publishedPort(postgres, 5432));
    }

    private final JdbcTemplate jdbcTemplate;
    private final HealthEndpoint healthEndpoint;

    @Autowired
    LocalDevEnvironmentTest(JdbcTemplate jdbcTemplate, HealthEndpoint healthEndpoint) {
        this.jdbcTemplate = jdbcTemplate;
        this.healthEndpoint = healthEndpoint;
    }

    // Acceptance criterion: docker compose up -d starts Postgres + Redis + payment mock
    @Test
    void composeStartsPostgresRedisAndPaymentMockHealthy() {
        EXPECTED_IMAGES.forEach((serviceName, image) -> {
            ContainerState container = service(serviceName);
            assertThat(container.isHealthy()).as("%s is healthy", serviceName).isTrue();
            assertThat(container.getContainerInfo().getConfig().getImage())
                    .as("%s image", serviceName)
                    .isEqualTo(image);
        });
    }

    // Acceptance criterion: docker compose up -d starts Postgres + Redis + payment mock
    @Test
    void paymentMockServesExpectationsFromTheRepository() throws Exception {
        ContainerState paSandbox = service("pa-sandbox");
        URI status = URI.create(
                "http://" + paSandbox.getHost() + ":" + publishedPort(paSandbox, 1080) + "/pa/status");

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response =
                    client.send(HttpRequest.newBuilder(status).GET().build(), HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"provider\" : \"mock\"");
        }
    }

    // Acceptance criterion: app connects to the local DB
    @Test
    void appConnectsToTheComposeDatabase() {
        Integer serverVersion =
                jdbcTemplate.queryForObject("select current_setting('server_version_num')::int", Integer.class);
        String pgvectorVersion = jdbcTemplate.queryForObject(
                "select default_version from pg_available_extensions where name = 'vector'", String.class);

        assertThat(serverVersion).isBetween(180000, 189999);
        assertThat(pgvectorVersion).isEqualTo("0.8.7");
        assertThat(healthEndpoint.healthForPath("db").getStatus()).isEqualTo(Status.UP);
    }

    /** The host port Docker picked for a compose {@code ports:} entry published as port 0. */
    private static int publishedPort(ContainerState container, int containerPort) {
        Ports.Binding[] bindings = container.getContainerInfo()
                .getNetworkSettings()
                .getPorts()
                .getBindings()
                .get(ExposedPort.tcp(containerPort));
        assertThat(bindings).as("published bindings for port %d", containerPort).isNotEmpty();
        return Integer.parseInt(bindings[0].getHostPortSpec());
    }

    private static ContainerState service(String serviceName) {
        return COMPOSE.getContainerByServiceName(serviceName)
                .orElseThrow(() -> new IllegalStateException("No container for compose service " + serviceName));
    }
}
