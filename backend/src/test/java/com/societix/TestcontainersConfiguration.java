package com.societix;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Supplies a real PostgreSQL 18 (with pgvector) container for tests that start the application
 * context, set up with the same two roles as docker-compose.yml.
 *
 * <ul>
 *   <li>The container's own user is the owner role. It is a superuser, owns the schema and is
 *       used only by Flyway to run the migrations.</li>
 *   <li>{@link #APP_RUNTIME_ROLE} is created by the repository's
 *       {@code infra/postgres/initdb/10-create-app-runtime-role.sh}. It is not a superuser and
 *       cannot bypass row-level security. The application's datasource connects as this role,
 *       so tests see the same RLS behaviour as the running application.</li>
 * </ul>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("pgvector/pgvector:0.8.7-pg18").asCompatibleSubstituteFor("postgres");

    static final String APP_RUNTIME_ROLE = "societix_app";
    private static final String APP_RUNTIME_PASSWORD = "societix_app";

    private static final String INIT_SCRIPT = "../infra/postgres/initdb/10-create-app-runtime-role.sh";

    @Bean
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE)
                .withEnv("APP_DB_USER", APP_RUNTIME_ROLE)
                .withEnv("APP_DB_PASSWORD", APP_RUNTIME_PASSWORD)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(INIT_SCRIPT, 0755),
                        "/docker-entrypoint-initdb.d/10-create-app-runtime-role.sh");
    }

    @Bean
    DynamicPropertyRegistrar postgresProperties(PostgreSQLContainer postgres) {
        return registry -> {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", () -> APP_RUNTIME_ROLE);
            registry.add("spring.datasource.password", () -> APP_RUNTIME_PASSWORD);
            registry.add("spring.flyway.user", postgres::getUsername);
            registry.add("spring.flyway.password", postgres::getPassword);
        };
    }
}
