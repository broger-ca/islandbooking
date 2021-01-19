package org.bin2.island.booking.api;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;

@MicronautTest(environments = "kafka")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseContainerTest implements TestPropertyProvider {
    private static PostgreSQLContainer postgreSQLContainer;
    private static KafkaContainer kafkaContainer;

    private void init() {
        if (postgreSQLContainer ==null) {
            postgreSQLContainer = new PostgreSQLContainer("postgres:9.6.12")
                    .withDatabaseName("test")
                    .withUsername("booking")
                    .withPassword("booking");
            postgreSQLContainer.start();
        }
        Flyway.configure()
                .dataSource(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword())
                .load().migrate();
        if (kafkaContainer==null) {
            kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"))
                    .withEmbeddedZookeeper();
            kafkaContainer.start();
        }
    }
    @Nonnull
    @Override
    public Map<String, String> getProperties() {
        init();
        var properties = new HashMap<String, String>();
        properties.put("vertx.pg.client.uri", "postgresql://" + postgreSQLContainer.getUsername()+":"+ postgreSQLContainer.getPassword()+"@"+ postgreSQLContainer.getJdbcUrl().substring("jdbc:postgresql://".length()));
        properties.put("kafka.bootstrap.servers",kafkaContainer.getBootstrapServers());
        return properties;
    }

    @Override
    public Map<String, String> get() {
        return getProperties();
    }
}
