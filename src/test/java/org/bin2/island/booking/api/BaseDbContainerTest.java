package org.bin2.island.booking.api;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;

@MicronautTest(propertySources = "test-application.yaml")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseDbContainerTest implements TestPropertyProvider {
    private PostgreSQLContainer container;


    private void init() {
        if (container==null) {
            container = new PostgreSQLContainer("postgres:9.6.12")
                    .withDatabaseName("test")
                    .withUsername("booking")
                    .withPassword("booking");
            container.start();
        }
        Flyway.configure()
                .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
                .load().migrate();
    }
    @Nonnull
    @Override
    public Map<String, String> getProperties() {
        init();
        var properties = new HashMap<String, String>();
        properties.put("vertx.pg.client.uri", "postgresql://" + container.getUsername()+":"+container.getPassword()+"@"+container.getJdbcUrl().substring("jdbc:postgresql://".length()));
        return properties;
    }

    @Override
    public Map<String, String> get() {
        return getProperties();
    }
}
