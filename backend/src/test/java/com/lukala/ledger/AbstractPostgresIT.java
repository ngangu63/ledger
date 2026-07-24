package com.lukala.ledger;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for integration tests. Starts a single Postgres container once and reuses
 * it across all IT classes (singleton-container pattern): the field is static and
 * started in a static initializer, not managed by the Testcontainers JUnit
 * extension, so it is not torn down between classes. Ryuk removes it at JVM exit.
 * Flyway migrates the schema on context start; {@code ddl-auto=validate} then
 * checks the JPA mappings against it.
 */
@SpringBootTest
public abstract class AbstractPostgresIT {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
