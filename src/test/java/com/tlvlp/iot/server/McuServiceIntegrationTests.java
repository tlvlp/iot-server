package com.tlvlp.iot.server;

import com.tlvlp.iot.server.mcu.Mcu;
import com.tlvlp.iot.server.mcu.McuService;
import com.tlvlp.iot.server.mqtt.GlobalTopics;
import com.tlvlp.iot.server.persistence.McuRepository;
import com.tlvlp.iot.server.testcontainers.MySqlTestContainersResource;
import com.tlvlp.iot.server.testcontainers.RabbitMqTestContainersResource;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(MySqlTestContainersResource.class)
@QuarkusTestResource(RabbitMqTestContainersResource.class)
public class McuServiceIntegrationTests {

    @Inject
    McuService mcuService;

    @Inject
    McuRepository mcuRepository;

    @Inject
    Vertx vertx;


    private MqttClient getMqttClient() {
        MqttClientOptions clientOptions = new MqttClientOptions()
                .setClientId("test")
                .setUsername(System.getProperty("MQTT_BROKER_USER"))
                .setPassword(System.getProperty("MQTT_BROKER_PASSWORD"))
                .setSsl(true);

        MqttClient client = MqttClient.create(vertx, clientOptions);
        client.connect(8883, System.getProperty("MQTT_BROKER_HOST"), event -> {});
        return client;
    }


    private Map<String, String> getUnitIdMap(String project, String unitName)  {
        return Map.of(
                "project", project,
                "unitName", unitName
        );
    }

    private Map<String, String> getModuleMap(String module, String name, String action, String value) {
        return Map.of(
                "module", module,
                "name", name,
                "action", action,
                "value", value
                );
    }

    private Object getMessagePayload(Map<String, String> unitIdMap, List<Map<String, String>> modulesList) {
        return JsonObject.mapFrom(
                        Map.of(
                        "id", unitIdMap,
                        "modules", modulesList
                ));
    }

    @Test
    @DisplayName("Save new MCU")
    public void unitSaveTest() {
        // given
        Object messagePayload = getMessagePayload(
                getUnitIdMap("project", "name"),
                List.of(
                        getModuleMap("module_1", "name_1", "action_1", "1"),
                        getModuleMap("module_2", "name_2", "action_2", "2")
                )
        );

        // when
        getMqttClient().publish(
                GlobalTopics.GLOBAL_STATUS.topic(),
                Json.encodeToBuffer(messagePayload),
                MqttQoS.AT_LEAST_ONCE,
                false,
                false
        );
        getMqttClient().disconnect();


        // then
        PanacheQuery<Mcu> repoAll = mcuRepository.findAll();
        assertThat(repoAll.stream().count())
                .as("Only one item is created.")
                .isEqualTo(1L);

        Mcu mcu = repoAll.firstResult();
        assertThat(mcu)
                .as("All fields are populated, including the Id")
                .hasFieldOrProperty("id");

    }

}
