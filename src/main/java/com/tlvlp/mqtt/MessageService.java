package com.tlvlp.mqtt;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.flogger.Flogger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.stream.Collectors;

@Flogger
@Startup
@ApplicationScoped
public class MessageService {

    private final String brokerHost;
    private final Integer brokerPort;
    private final Integer brokerQoS;

    private final MqttClient mqttClient;
    private final ObjectMapper mapper;

    public MessageService(Vertx vertx,
                          @ConfigProperty(name = "mqtt.broker.host") String brokerHost,
                          @ConfigProperty(name = "mqtt.broker.port") Integer brokerPort,
                          @ConfigProperty(name = "mqtt.broker.username") String brokerUser,
                          @ConfigProperty(name = "mqtt.broker.password") String brokerPassword,
                          @ConfigProperty(name = "mqtt.broker.qos", defaultValue = "1") Integer brokerQoS
    ) {
        this.mapper = new ObjectMapper();
        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.brokerQoS = brokerQoS;

        log.atInfo().log("Creating MQTT client");
        var clientOptions = new MqttClientOptions()
                .setClientId("tlvlp-iot-server")
                .setUsername(brokerUser.strip())
                .setPassword(brokerPassword.strip())
                .setWillFlag(true)
                .setWillMessage("tlvlp-iot-server lost connection.")
                .setSsl(true);
        this.mqttClient = MqttClient.create(vertx, clientOptions);
    }

    @PostConstruct
    public void connect() {
        log.atInfo().log("Connecting to the MQTT broker at %s:%d", brokerHost, brokerPort);
        mqttClient.connect(brokerPort, brokerHost.strip(), event -> {
            if(event.failed()) {
                throw new RuntimeException("Connection failed.");
            }
            log.atInfo().log("Subscribing to global topics: %s", GlobalTopics.values());
            var topicQosMap = GlobalTopics.getAll().collect(Collectors.toMap(topic -> topic, na -> brokerQoS));
            mqttClient
                    .exceptionHandler(this::errorHandler)
                    .publishHandler(this::messageHandler)
                    .subscribe(topicQosMap);
        });
    }

    private void errorHandler(Throwable throwable) {
        log.atSevere().withCause(throwable).log("Mqtt client exception.");
    }

    private void messageHandler(MqttPublishMessage message) {
        try {
            var payload = mapper.readTree(message.payload().getBytes());
            log.atInfo().log("Message received on topic:%s with payload:%s", message.topicName(), payload);
        } catch (IOException e) {
            log.atSevere().withCause(e).log("Unable to deserialize MQTT message contents.");
        }
    }
}
