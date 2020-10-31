package com.tlvlp.iot.server.health;

import com.tlvlp.iot.server.mqtt.MessageService;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import javax.inject.Singleton;

@Readiness
@Singleton
public class MqttConnectionHealthCheck implements HealthCheck {

    private final MessageService messageService;

    public MqttConnectionHealthCheck(MessageService messageService) {
        this.messageService = messageService;
    }


    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Mqtt connection");
        if (messageService.isBrokerConnected()) {
            responseBuilder.up();
        } else {
            responseBuilder.down();
        }
        return responseBuilder.build();
    }
}
