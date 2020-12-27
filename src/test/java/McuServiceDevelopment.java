import com.tlvlp.iot.server.mqtt.GlobalTopics;
import com.tlvlp.iot.server.mqtt.Message;
import com.tlvlp.iot.server.mcu.McuService;
import com.tlvlp.iot.server.mcu.Unit;
import com.tlvlp.iot.server.persistence.UnitRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@Transactional
public class McuServiceDevelopment {

    @Inject
    McuService mcuService;

    @Inject
    UnitRepository unitRepository;

    @Inject
    EventBus eventBus;

//    @BeforeEach
//    void setup() {
//        //reset db
//    }

    private Message getNewUnitMessage() {
        return new Message()
                .topic(GlobalTopics.GLOBAL_STATUS.topic())
                .payload(JsonObject.mapFrom(Map.of(
                        "id", Map.of(
                                "project", "tlvlp_iot_test",
                                "unitName", "garden_2020_test"
                        ),
                        "modules", List.of(
                                Map.of(
                                        "value", 1,
                                        "name", "growlight_test_1",
                                        "module", "relay"),
                                Map.of(
                                        "value", 0,
                                        "name", "growlight_test_2",
                                        "module", "relay")
                        )
                )));
    }

    @Test
    @DisplayName("Save new unit")
    public void unitSaveTest() {
        // given
        Message message = getNewUnitMessage();

        // when
        IntStream.range(0, 3).forEach(i ->
                mcuService.handleIngressMessage(message)
//                eventBus.sendAndForget("mqtt_ingress", message)
        );

        // then
        var repoAll = unitRepository.findAll();
//        assertThat(repoAll.stream().count())
//                .as("Only one item is created.")
//                .isEqualTo(1L);

        Unit unit = repoAll.firstResult();
        assertThat(unit)
                .as("All fields are populated, including the Id")
                .hasFieldOrProperty("id");

    }

}
