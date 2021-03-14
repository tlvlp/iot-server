package com.tlvlp.iot.server.testcontainers;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.util.Map;

@Slf4j
public class RabbitMqTestContainersResource implements QuarkusTestResourceLifecycleManager {


    private static RabbitMQContainer brokerContainer = new RabbitMQContainer("rabbitmq:3.8.14-alpine")
            .withPluginsEnabled("rabbitmq_mqtt")
            .withLogConsumer(new Slf4jLogConsumer(log));

    @Override
    public Map<String, String> start() {
        brokerContainer.start();
        return Map.of(
                "MQTT_BROKER_HOST", brokerContainer.getAmqpsUrl(),
                "MQTT_BROKER_USER", brokerContainer.getAdminUsername(),
                "MQTT_BROKER_PASSWORD", brokerContainer.getAdminPassword()
                );
    }

    @Override
    public void stop() {
        brokerContainer.stop();
    }

    @Override
    public int order() {
        return 1;
    }
}
