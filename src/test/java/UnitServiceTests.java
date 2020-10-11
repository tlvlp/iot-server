import com.tlvlp.mqtt.GlobalTopics;
import com.tlvlp.mqtt.Message;
import com.tlvlp.units.Unit;
import com.tlvlp.units.UnitRepository;
import com.tlvlp.units.UnitService;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
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
public class UnitServiceTests {

    @Inject
    UnitService unitService;

    @Inject
    UnitRepository unitRepository;

    @Inject
    EventBus eventBus;

    @BeforeEach
    void setup() {

        unitRepository.deleteAll();
    }

    private Message getNewUnitMessage(Integer value) {
        return new Message()
                .topic(GlobalTopics.GLOBAL_STATUS.topic())
                .payload(JsonObject.mapFrom(Map.of(
                        "id", Map.of(
                                "project", "tlvlp_iot_test",
                                "unitName", "garden_2020_test"
                        ),
                        "modules", List.of(
                                Map.of(
                                        "value", value,
                                        "name", "growlight_test",
                                        "module", "relay")
                        )
                )));
    }

    @Test
    @DisplayName("Save new unit")
    public void unitSaveTest() {
        // given
        Message message = getNewUnitMessage(1);

        // when
        IntStream.range(1, 3).forEach(i ->
//                eventBus.publish("mqtt_ingress", message)
                        unitService.handleIngressMessage(message)
        );

        // then
        var repoAll = unitRepository.findAll();
        assertThat(repoAll.stream().count())
                .as("Only one item is created.")
                .isEqualTo(1L);

        Unit unit = repoAll.firstResult();
        assertThat(unit)
                .as("All fields are populated, including the Id")
                .hasNoNullFieldsOrProperties();

    }

}
