package com.affiliate.rentals.gydi;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for integration tests.
 *
 * <p>This class configures a PostgreSQL TestContainer that will be shared across all integration tests.
 * TestContainers automatically starts a PostgreSQL instance in Docker for testing.
 *
 * <p>Usage: Extend this class for any integration test that needs a full application context
 * with database connectivity.
 *
 * <p>Benefits:
 * <ul>
 *   <li>Tests run against actual PostgreSQL instead of H2, ensuring database compatibility</li>
 *   <li>Supports PostgreSQL-specific features (JSONB, ENUM types, etc.)</li>
 *   <li>Container is reused across all tests for better performance</li>
 *   <li>Automatically cleans up after tests</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    /**
     * PostgreSQL container for integration tests.
     *
     * <p>Using @ServiceConnection automatically configures Spring Boot to use this container
     * as the datasource. No manual configuration needed.
     *
     * <p>The container is static, so it's shared across all test classes that extend this base class.
     * This significantly improves test performance.
     */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // Reuse container across test runs for faster startup
}
