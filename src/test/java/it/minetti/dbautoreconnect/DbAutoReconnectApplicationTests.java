package it.minetti.dbautoreconnect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class DbAutoReconnectApplicationTests {

    @LocalServerPort
    int port;

    @Container
    private static final PostgreSQLContainer db = new PostgreSQLContainer("postgres:11.1");

    @Autowired
    private DbEntityRepository repo;

    @Autowired
    TestRestTemplate template;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void contextLoads() throws InterruptedException {
        log.info("Starting tests on port {}.", port);

        log.info("Saving two rows in the database...");
        repo.save(new DbEntity("first"));
        repo.save(new DbEntity("second"));
        log.info(" ... done: {}", repo.findAll());

        assertThat(isHealthy(), is(true));
        assertThat(getEntityIdsFromDb(), hasSize(2));

        pause(db);

        assertThat(isHealthy(), is(false));
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
        IntStream.range(1, 20).parallel().forEach(
                i -> assertThat(getEntityIdsFromDb(), is(nullValue()))
        );
        assertThat(isHealthy(), is(false));

        unpause(db);

        assertThat(isHealthy(), is(true));
        assertThat(getEntityIdsFromDb(), hasSize(2));
        assertThat(isHealthy(), is(true));
    }

    @SneakyThrows
    private boolean isHealthy() {
        var healthEntity = template.getForEntity("http://localhost:{port}/actuator/health", StatusWrapper.class, port);
        log.info("Health check http status is {} and status is {}", healthEntity.getStatusCode(), healthEntity.getBody().getStatus());
        if (healthEntity.getBody().getComponent() != null) {
            log.info(" ... and database details: {}", healthEntity.getBody().getComponent().getDb().getDetails());
        }
        return healthEntity.getStatusCode().is2xxSuccessful();
    }

    @SneakyThrows
    private List<String> getEntityIdsFromDb() {
        var idsEntity = template.getForEntity("http://localhost:{port}/entities", String.class, port);
        log.info("Query for database entities has status {}", idsEntity.getStatusCode());
        log.info(" ... and entity body: {}", idsEntity.getBody());
        TypeReference<List<String>> listOfStringType = new TypeReference<>() {
        };
        return idsEntity.getStatusCode().is2xxSuccessful() ? objectMapper.readValue(idsEntity.getBody(), listOfStringType) : null;
    }

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        log.info("Setting up the datasource using url {}.", db.getJdbcUrl());
        registry.add("spring.datasource.url", db::getJdbcUrl);
        registry.add("spring.datasource.password", db::getPassword);
        registry.add("spring.datasource.username", db::getUsername);
    }

    @Data
    public static class StatusWrapper {
        private String status;
        private StatusWrapper details;
        private StatusComponent component;

        @Data
        public static class StatusComponent {
            private StatusWrapper db;
        }
    }

    public void pause(GenericContainer container) throws InterruptedException {
        log.info("Pausing container {} ...", container.getDockerImageName());
        container.getDockerClient().pauseContainerCmd(container.getContainerId()).exec();
        TimeUnit.SECONDS.sleep(2);
        log.info(" ... paused.");
    }

    public void unpause(GenericContainer container) throws InterruptedException {
        log.info("Unpausing container {} ...", container.getDockerImageName());
        container.getDockerClient().unpauseContainerCmd(container.getContainerId()).exec();
        TimeUnit.SECONDS.sleep(2);
        log.info(" ... unpaused.");
    }

}
