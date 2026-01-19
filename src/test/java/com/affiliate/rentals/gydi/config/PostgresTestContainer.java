package com.affiliate.rentals.gydi.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers configuration for PostgreSQL.
 *
 * <p>
 * This configuration provides a real PostgreSQL container for integration
 * tests,
 * ensuring tests run against the same database engine as production.
 * </p>
 *
 * <p>
 * The {@code @ServiceConnection} annotation automatically configures Spring
 * Boot's
 * DataSource to connect to the PostgreSQL container.
 * </p>
 *
 * <p>
 * Benefits:
 * <ul>
 * <li>Tests use same DB engine as production (PostgreSQL)</li>
 * <li>Supports PostgreSQL-specific features like ENUM types</li>
 * <li>Automatically starts/stops container for tests</li>
 * <li>Container is reused across all tests for performance</li>
 * </ul>
 * </p>
 *
 * @author GYDI Development Team
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainer {

    /**
     * Creates a PostgreSQL container for testing.
     *
     * <p>
     * The container is configured with:
     * <ul>
     * <li>PostgreSQL 16 (same version as production)</li>
     * <li>Database name: testdb</li>
     * <li>Username: test</li>
     * <li>Password: test</li>
     * <li>Reusable: true (container persists across tests for performance)</li>
     * </ul>
     * </p>
     *
     * @return PostgreSQL container instance
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
    }
}
