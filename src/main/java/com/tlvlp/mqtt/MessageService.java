package com.tlvlp.mqtt;


import com.tlvlp.units.UnitService;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mutiny.core.eventbus.EventBus;
import lombok.extern.flogger.Flogger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.util.stream.Collectors;

@Flogger
@Startup
@ApplicationScoped
public class MessageService {

    private Boolean isBrokerConnected;
    private Boolean isServiceDisabled;

    private final String brokerHost;
    private final Integer brokerPort;
    private final Integer brokerQoS;

    private final EventBus eventBus;
    private final MqttClient mqttClient;

    public MessageService(Vertx vertx,
                          EventBus eventBus,
                          @ConfigProperty(name = "mqtt.message.service.disabled", defaultValue = "false") Boolean isServiceDisabled,
                          @ConfigProperty(name = "mqtt.broker.host") String brokerHost,
                          @ConfigProperty(name = "mqtt.broker.port") Integer brokerPort,
                          @ConfigProperty(name = "mqtt.broker.username") String brokerUser,
                          @ConfigProperty(name = "mqtt.broker.password") String brokerPassword,
                          @ConfigProperty(name = "mqtt.broker.qos", defaultValue = "1") Integer brokerQoS
    ) {
        this.eventBus = eventBus;
        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.brokerQoS = brokerQoS;
        this.isBrokerConnected = false;
        this.isServiceDisabled = isServiceDisabled;

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
    void connect() {
        if(isServiceDisabled) {
            log.atInfo().log("Message service is disabled. MQTT Broker connection is skipped.");
            return;
        }
        log.atInfo().log("Connecting to the MQTT broker at %s:%d", brokerHost, brokerPort);
        mqttClient.connect(brokerPort, brokerHost.strip(), event -> {
            if(event.failed()) {
                throw new RuntimeException("Connection failed.");
            }
            subscribeToGlobalTopics();
            sendGlobalStatusRequest();
        });
    }

    private void subscribeToGlobalTopics() {
        var topicQosMap = GlobalTopics.getIngressTopicStream()
                .collect(Collectors.toMap(topic -> topic, na -> brokerQoS));
        log.atInfo().log("Subscribing to global ingress topics: %s", topicQosMap.keySet());

        mqttClient
                .exceptionHandler(err -> log.atSevere().withCause(err).log("Mqtt client exception."))
                .publishHandler(mqttMessage -> {
                    var message = new Message()
                            .topic(mqttMessage.topicName())
                            .payload(mqttMessage.payload().toJsonObject());
                    log.atInfo().log("Message received: %s", message);
                    eventBus.sendAndForget("mqtt_ingress", message);
                })
                .subscribe(topicQosMap);

        isBrokerConnected = true;
    }

    public Boolean isBrokerConnected() {
        return isBrokerConnected;
    }

    public void sendGlobalStatusRequest() {
        log.atInfo().log("Sending a global status request for all available units to check in.");
        sendMessage(GlobalTopics.GLOBAL_STATUS_REQUEST.topic(), Buffer.buffer());
    }

    public void sendMessage(String topic, Buffer body) {
        if(isServiceDisabled) {
            log.atInfo().log("Message service is disabled. Not sending message. Topic:%s Body:%s", topic, body.toJsonObject());
            return;
        }
        mqttClient.publish(
                topic,
                body,
                MqttQoS.valueOf(brokerQoS),
                false,
                false
        );
    }
}
