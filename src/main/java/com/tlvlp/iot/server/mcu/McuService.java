package com.tlvlp.iot.server.mcu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlvlp.iot.server.mqtt.GlobalTopics;
import com.tlvlp.iot.server.mqtt.Message;
import com.tlvlp.iot.server.mqtt.MessageService;
import com.tlvlp.iot.server.persistence.ModuleRepository;
import com.tlvlp.iot.server.persistence.McuLogRepository;
import com.tlvlp.iot.server.persistence.McuRepository;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import lombok.extern.flogger.Flogger;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Startup
@Flogger
@ApplicationScoped
public class McuService {

    private final EventBus eventBus;
    private final ObjectMapper jsonMapper;
    private final MessageService messageService;
    private final McuRepository mcuRepository;
    private final ModuleRepository moduleRepository;
    private final McuLogRepository mcuLogRepository;

    public McuService(EventBus eventBus,
                      ObjectMapper jsonMapper,
                      MessageService messageService,
                      McuRepository mcuRepository,
                      ModuleRepository moduleRepository,
                      McuLogRepository mcuLogRepository) {
        this.eventBus = eventBus;
        this.jsonMapper = jsonMapper;
        this.messageService = messageService;
        this.mcuRepository = mcuRepository;
        this.moduleRepository = moduleRepository;
        this.mcuLogRepository = mcuLogRepository;
    }

    public Multi<Mcu> getAllMcus() {
        return Multi.createFrom().items(mcuRepository.streamAll())
                .onFailure().invoke(e -> log.atSevere().log("Unable to get all mcus: %s", e.getMessage()));
    }

    public Uni<Mcu> getMcuById(Long mcuId) {
        return Uni.createFrom().item(mcuRepository.findById(mcuId))
                .onFailure().invoke(e -> log.atSevere().log("Unable to get mcu by id(%s): %s", mcuId, e.getMessage()));
    }

    public Multi<McuLog> getMcuLogsByMcuId(Long mcuId) {
        return Multi.createFrom().items(mcuLogRepository.findAllByMcuId(mcuId).stream())
                .onFailure().invoke(e -> log.atSevere().log("Unable to get mcu logs by mcu id(%s): %s", mcuId, e.getMessage()));
    }

    public Multi<Module> getModulesByMcuId(Long mcuId) {
        return Multi.createFrom().items(moduleRepository.findAllByMcuId(mcuId).stream())
                .onFailure().invoke(e -> log.atSevere().log("Unable to get modules by mcu id(%s): %s", mcuId, e.getMessage()));
    }

    @ConsumeEvent(value = "mcu_control", blocking = true)
    public Uni<Void> sendScheduledControlMessages(String moduleControlsJson) {
        try {
            List<Module> moduleControlsAll = jsonMapper.readValue(moduleControlsJson, new TypeReference<>() {});
            sendControlMessages(moduleControlsAll);
        } catch (JsonProcessingException e) {
            var err = String.format("Unable to send module control messages. Cannot parse message contents: %s", moduleControlsJson);
            log.atSevere().log(err);

        }
        return Uni.createFrom().voidItem();
    }

    /**
     * Sends out mcu control messages for any number of MCUs and modules.
     * Note: If a module has more than one instance in the list, then the execution order is not guaranteed.
     *
     * @param moduleControlsAll a list of modified {@link Module}s that will be converted and sent out to control the MCUs.
     * @return void.
     */
    public Uni<Void> sendControlMessages(List<Module> moduleControlsAll) {
        try {
            Map<Long, List<ModuleDTO>> modulesByMcuIds = moduleControlsAll.stream()
                    .collect(groupingBy(Module::getMcuId, mapping(this::convertModuleToModuleDTO, toList())));

            modulesByMcuIds.forEach((mcuId, moduleControls) -> {
                Mcu mcu = mcuRepository.findById(mcuId);
                if (mcu == null) {
                    log.atSevere().log("Unable to send module control messages to non-existent mcu Id:%s, modules:%s",
                            mcuId, moduleControls);
                    return;
                }

                Buffer body = Json.encodeToBuffer(moduleControls);
                messageService.sendMessage(getControlTopic(mcu), body);

                var mcuLog = new McuLog()
                        .setMcuId(mcu.getId())
                        .setTimeUtc(ZonedDateTime.now(ZoneOffset.UTC))
                        .setType(McuLog.Type.OUTGOING_CONTROL)
                        .setLogEntry(Json.encodePrettily(moduleControlsAll));
                mcuLogRepository.save(mcuLog);
            });

            return Uni.createFrom().voidItem();
        } catch (Exception e) {
            return Uni.createFrom().failure(new McuException(
                    String.format("Unable to send mcu control message! Module controls:%s %n%s",
                            moduleControlsAll, e.getMessage())));
        }

    }

    @ConsumeEvent(value = "mqtt_ingress", blocking = true)
    @Transactional
    public Uni<Void> handleIngressMessage(Message message) {
        try {
            log.atFine().log("Message event received: %s", message);
            var body = message.payload();
            var topic = message.topic();
            if (topic.equals(GlobalTopics.GLOBAL_STATUS.topic())) {
                handleStatusMessage(body);
            } else if (topic.equals(GlobalTopics.GLOBAL_INACTIVE.topic())) {
                handleInactiveMessage(body);
            } else if (topic.equals(GlobalTopics.GLOBAL_ERROR.topic())) {
                handleErrorMessage(body);
            } else {
                log.atSevere().log("Unrecognized topic name: %s", topic);
            }
            return Uni.createFrom().voidItem();
        } catch (Exception e) {
            log.atSevere().log("Unable to handle ingress message: %s", e.getMessage());
            return Uni.createFrom().failure(e);
        }
    }

    private void handleErrorMessage(JsonObject body) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);
        var mcu = getOrCreateMcuFromBody(body)
                .setActive(true)
                .setLastSeenUtc(timeUtc);

        var error = Optional.ofNullable(body.getString("error"))
                .orElseGet(() -> {
                    log.atSevere().log("Missing error message for mcu: %s", mcu);
                    return "Error message is missing!";
                });
        var mcuLog = new McuLog()
                .setMcuId(mcu.getId())
                .setTimeUtc(timeUtc)
                .setType(McuLog.Type.INCOMING_ERROR)
                .setLogEntry(error);
        mcuLogRepository.save(mcuLog);

        eventBus.publish("mcu_error", Map.of(
                "mcu", mcu,
                "error", error));
    }

    private void handleInactiveMessage(JsonObject body) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);
        var mcu = getOrCreateMcuFromBody(body)
                .setActive(false);
        if (mcu.getLastSeenUtc() == null) {
            // Keep last seen data if present.
            mcu.setLastSeenUtc(ZonedDateTime.now(ZoneOffset.UTC));
        }
        var mcuSaved = mcuRepository.saveAndFlush(mcu);

        var mcuLog = new McuLog()
                .setMcuId(mcuSaved.getId())
                .setTimeUtc(timeUtc)
                .setType(McuLog.Type.INCOMING_INACTIVE)
                .setLogEntry("MCU is inactive.");
        mcuLogRepository.save(mcuLog);

        eventBus.publish("mcu_inactive", mcuSaved);
    }

    private void handleStatusMessage(JsonObject body) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);
        var mcu = getOrCreateMcuFromBody(body)
                .setActive(true)
                .setLastSeenUtc(timeUtc);
        var savedMcu = mcuRepository.saveAndFlush(mcu);
        updateOrCreateModulesFromBody(savedMcu, body);
    }

    private Mcu getOrCreateMcuFromBody(JsonObject body) {
        var idJson = body.getJsonObject("id");
        var project = idJson.getString("project");
        var name = idJson.getString("mcuName");

        return mcuRepository
                .findByProjectAndName(project, name)
                .orElseGet(() -> createAndPersistNewMcu(project, name));
    }

    private Mcu createAndPersistNewMcu(String project, String name) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);

        var mcu = new Mcu()
                .setProject(project)
                .setName(name)
                .setActive(true)
                .setLastSeenUtc(timeUtc);
        var mcuSaved = mcuRepository.saveAndFlush(mcu);

        var mcuLog = new McuLog()
                .setMcuId(mcuSaved.getId())
                .setTimeUtc(timeUtc)
                .setType(McuLog.Type.STATUS_CHANGE)
                .setLogEntry("New MCU was registered.");
        mcuLogRepository.save(mcuLog);

        log.atInfo().log("New MCU was registered: %s", mcuSaved);

        return mcuSaved;
    }

    private String getControlTopic(Mcu mcu) {
        return String.format("/mcu/%s-%s/control", mcu.getProject(), mcu.getName());
    }

    private void updateOrCreateModulesFromBody(Mcu mcu, JsonObject body) {
        Long mcuId = mcu.getId();
        var newModules = body.getJsonArray("modules")
                .stream()
                .map(moduleDtoObj -> Json.decodeValue(String.valueOf(moduleDtoObj), ModuleDTO.class))
                .map(moduleDTO -> updateOrCreateModule(mcuId, moduleDTO))
                .collect(Collectors.toSet());

        // Check for modules that are active in the DB but are no longer present in the status summary and inactivate them.
        moduleRepository.getAllActiveModulesByMcuId(mcuId)
                .stream()
                .filter(module -> !newModules.contains(module))
                .forEach(module -> {
                    module.setActive(false);
                    moduleRepository.save(module);

                    var updateMessage = String.format("Module was inactivated: %s", module);

                    log.atInfo().log(updateMessage);

                    var mcuLog = new McuLog()
                            .setMcuId(mcuId)
                            .setTimeUtc(ZonedDateTime.now(ZoneOffset.UTC))
                            .setType(McuLog.Type.STATUS_CHANGE)
                            .setLogEntry(updateMessage);
                    mcuLogRepository.save(mcuLog);
                });
    }

    private Module updateOrCreateModule(Long mcuId, ModuleDTO dto) {
        var moduleType = dto.getModule();
        var name = dto.getName();
        var value = dto.getValue();

        var moduleDb = moduleRepository.findByMcuIdAndModuleAndName(mcuId, moduleType, name)
                .orElseGet(() -> createAndPersistNewModule(mcuId, moduleType, name, value));

        // Reactivation
        if (!moduleDb.getActive().equals(true)) {
            moduleDb.setActive(true);
            var msg = String.format("Module was reactivated: %s", moduleDb);
            var mcuLog = new McuLog()
                    .setMcuId(mcuId)
                    .setTimeUtc(ZonedDateTime.now(ZoneOffset.UTC))
                    .setType(McuLog.Type.STATUS_CHANGE)
                    .setLogEntry(msg);
            mcuLogRepository.save(mcuLog);
            log.atInfo().log(msg);
        }

        // Value change
        if (!moduleDb.getValue().equals(value)) {
            moduleDb.setValue(value);
            log.atFine().log("Module value was updated: %s", moduleDb);
        }

        return moduleDb;
    }

    private Module createAndPersistNewModule(Long mcuId, String moduleType, String name, Double value) {
        var module = new Module()
                .setMcuId(mcuId)
                .setModule(moduleType)
                .setName(name)
                .setValue(value)
                .setActive(true);
        var moduleSaved = moduleRepository.saveAndFlush(module);

        var newModuleMessage = String.format("New Module was registered: %s", moduleSaved);

        var mcuLog = new McuLog()
                .setMcuId(mcuId)
                .setTimeUtc(ZonedDateTime.now(ZoneOffset.UTC))
                .setType(McuLog.Type.STATUS_CHANGE)
                .setLogEntry(newModuleMessage);
        mcuLogRepository.save(mcuLog);

        log.atInfo().log(newModuleMessage);

        return moduleSaved;
    }

    private ModuleDTO convertModuleToModuleDTO(Module module) {
        return new ModuleDTO()
                .setModule(module.getModule())
                .setName(module.getName())
                .setValue(module.getValue());
    }

}
