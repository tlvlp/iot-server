package com.tlvlp.iot.server.testcontainers;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import java.util.Map;

@Slf4j
public class MySqlTestContainersResource implements QuarkusTestResourceLifecycleManager {

    private static MySQLContainer dbContainer = new MySQLContainer<>("mysql:8.0.23")
            .withUsername("root")
            .withPassword("root")
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withClasspathResourceMapping("db_init_scripts", "/docker-entrypoint-initdb.d", BindMode.READ_ONLY);

    @Override
    public Map<String, String> start() {
        dbContainer.start();
        return Map.of(
                "DB_HOST", dbContainer.getJdbcUrl(),
                "DB_USER", dbContainer.getUsername(),
                "DB_PASS", dbContainer.getPassword()
        );
    }

    @Override
    public void stop() {
        dbContainer.stop();
    }

    @Override
    public int order() {
        return 0;
    }
}
