package com.tlvlp.iot.server.mcu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tlvlp.iot.server.mqtt.GlobalTopics;
import com.tlvlp.iot.server.mqtt.Message;
import com.tlvlp.iot.server.mqtt.MessageService;
import com.tlvlp.iot.server.persistence.ModuleRepository;
import com.tlvlp.iot.server.persistence.UnitLogRepository;
import com.tlvlp.iot.server.persistence.UnitRepository;
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
    private final UnitRepository unitRepository;
    private final ModuleRepository moduleRepository;
    private final UnitLogRepository unitLogRepository;

    public McuService(EventBus eventBus,
                      ObjectMapper jsonMapper,
                      MessageService messageService,
                      UnitRepository unitRepository,
                      ModuleRepository moduleRepository,
                      UnitLogRepository unitLogRepository) {
        this.eventBus = eventBus;
        this.jsonMapper = jsonMapper;
        this.messageService = messageService;
        this.unitRepository = unitRepository;
        this.moduleRepository = moduleRepository;
        this.unitLogRepository = unitLogRepository;
    }

    public Multi<Unit> getAllUnits() {
        return Multi.createFrom().items(unitRepository.streamAll())
                .onFailure().invoke(e -> log.atSevere().log("Unable to get all units: %s", e.getMessage()));
    }

    public Uni<Unit> getUnitById(Long unitId) {
        return Uni.createFrom().item(unitRepository.findById(unitId))
                .onFailure().invoke(e -> log.atSevere().log("Unable to get unit by id(%s): %s", unitId, e.getMessage()));
    }

    public Multi<UnitLog> getUnitLogsByUnitId(Long unitId) {
        return Multi.createFrom().items(unitLogRepository.findAllByUnitId(unitId).stream())
                .onFailure().invoke(e -> log.atSevere().log("Unable to get unit logs by unit id(%s): %s", unitId, e.getMessage()));
    }

    public Multi<Module> getModulesByUnitId(Long unitId) {
        return Multi.createFrom().items(moduleRepository.findAllByUnitId(unitId).stream())
                .onFailure().invoke(e -> log.atSevere().log("Unable to get modules by unit id(%s): %s", unitId, e.getMessage()));
    }

    @ConsumeEvent(value = "unit_control", blocking = true)
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
     * Sends out unit control messages for any number of units and modules.
     * Note: If a module has more than one instance in the list, then the execution order is not guaranteed.
     *
     * @param moduleControlsAll a list of modified {@link Module}s that will be converted and sent out to control the MCUs.
     * @return void.
     */
    public Uni<Void> sendControlMessages(List<Module> moduleControlsAll) {
        try {
            Map<Long, List<ModuleDTO>> modulesByUnitIds = moduleControlsAll.stream()
                    .collect(groupingBy(Module::getUnitId, mapping(this::convertModuleToModuleDTO, toList())));

            modulesByUnitIds.forEach((unitId, moduleControls) -> {
                Unit unit = unitRepository.findById(unitId);
                if (unit == null) {
                    log.atSevere().log("Unable to send module control messages to non-existent unit Id:%s, modules:%s",
                            unitId, moduleControls);
                    return;
                }

                Buffer body = Json.encodeToBuffer(moduleControls);
                messageService.sendMessage(getControlTopic(unit), body);

                var unitLog = new UnitLog()
                        .setUnitId(unit.getId())
                        .setTimeUtc(ZonedDateTime.now(ZoneOffset.UTC))
                        .setType(UnitLog.Type.OUTGOING_CONTROL)
                        .setLogEntry(Json.encodePrettily(moduleControlsAll));
                unitLogRepository.save(unitLog);
            });

            return Uni.createFrom().voidItem();
        } catch (Exception e) {
            return Uni.createFrom().failure(new UnitException(
                    String.format("Unable to send unit control message! Module controls:%s %n%s",
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
        var unit = getOrCreateUnitFromBody(body)
                .setActive(true)
                .setLastSeenUtc(timeUtc);

        var error = Optional.ofNullable(body.getString("error"))
                .orElseGet(() -> {
                    log.atSevere().log("Missing error message for unit: %s", unit);
                    return "Error message is missing!";
                });
        var unitLog = new UnitLog()
                .setUnitId(unit.getId())
                .setTimeUtc(timeUtc)
                .setType(UnitLog.Type.INCOMING_ERROR)
                .setLogEntry(error);
        unitLogRepository.save(unitLog);

        eventBus.publish("unit_error", Map.of(
                "unit", unit,
                "error", error));
    }

    private void handleInactiveMessage(JsonObject body) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);
        var unit = getOrCreateUnitFromBody(body)
                .setActive(false);
        if (unit.getLastSeenUtc() == null) {
            // Keep last seen data if present.
            unit.setLastSeenUtc(ZonedDateTime.now(ZoneOffset.UTC));
        }
        var unitSaved = unitRepository.saveAndFlush(unit);

        var unitLog = new UnitLog()
                .setUnitId(unitSaved.getId())
                .setTimeUtc(timeUtc)
                .setType(UnitLog.Type.INCOMING_INACTIVE)
                .setLogEntry("Unit is inactive.");
        unitLogRepository.save(unitLog);

        eventBus.publish("unit_inactive", unitSaved);
    }

    private void handleStatusMessage(JsonObject body) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);
        var unit = getOrCreateUnitFromBody(body)
                .setActive(true)
                .setLastSeenUtc(timeUtc);
        var savedUnit = unitRepository.saveAndFlush(unit);
        updateOrCreateModulesFromBody(savedUnit, body);
    }

    private Unit getOrCreateUnitFromBody(JsonObject body) {
        var idJson = body.getJsonObject("id");
        var project = idJson.getString("project");
        var name = idJson.getString("unitName");

        return unitRepository
                .findByProjectAndName(project, name)
                .orElseGet(() -> createAndPersistNewUnit(project, name));
    }

    private Unit createAndPersistNewUnit(String project, String name) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);

        var unit = new Unit()
                .setProject(project)
                .setName(name)
                .setActive(true)
                .setLastSeenUtc(timeUtc);
        var unitSaved = unitRepository.saveAndFlush(unit);

        var unitLog = new UnitLog()
                .setUnitId(unitSaved.getId())
                .setTimeUtc(timeUtc)
                .setType(UnitLog.Type.STATUS_CHANGE)
                .setLogEntry("New Unit was registered.");
        unitLogRepository.save(unitLog);

        log.atInfo().log("New Unit was registered: %s", unitSaved);

        return unitSaved;
    }

    private String getControlTopic(Unit unit) {
        return String.format("/units/%s-%s/control", unit.getProject(), unit.getName());
    }

    private void updateOrCreateModulesFromBody(Unit unit, JsonObject body) {
        Long unitId = unit.getId();
        var newModules = body.getJsonArray("modules")
                .stream()
                .map(moduleDtoObj -> Json.decodeValue(String.valueOf(moduleDtoObj), ModuleDTO.class))
                .map(moduleDTO -> updateOrCreateModule(unitId, moduleDTO))
                .collect(Collectors.toSet());

        // Check for modules that are active in the DB but are no longer present in the status summary and inactivate them.
        moduleRepository.getAllActiveModulesByUnitId(unitId)
                .stream()
                .filter(module -> !newModules.contains(module))
                .forEach(module -> {
                    module.setActive(false);
                    moduleRepository.save(module);

                    var updateMessage = String.format("Module was inactivated: %s", module);

                    log.atInfo().log(updateMessage);

                    var unitLog = new UnitLog()
                            .setUnitId(unitId)
                            .setTimeUtc(ZonedDateTime.now(ZoneOffset.UTC))
                            .setType(UnitLog.Type.STATUS_CHANGE)
                            .setLogEntry(updateMessage);
                    unitLogRepository.save(unitLog);
                });
    }

    private Module updateOrCreateModule(Long unitId, ModuleDTO dto) {
        var moduleType = dto.getModule();
        var name = dto.getName();
        var value = dto.getValue();

        var moduleDb = moduleRepository.findByUnitIdAndModuleAndName(unitId, moduleType, name)
                .orElseGet(() -> createAndPersistNewModule(unitId, moduleType, name, value));

        // Reactivation
        if (!moduleDb.getActive().equals(true)) {
            moduleDb.setActive(true);
            var msg = String.format("Module was reactivated: %s", moduleDb);
            var unitLog = new UnitLog()
                    .setUnitId(unitId)
                    .setTimeUtc(ZonedDateTime.now(ZoneOffset.UTC))
                    .setType(UnitLog.Type.STATUS_CHANGE)
                    .setLogEntry(msg);
            unitLogRepository.save(unitLog);
            log.atInfo().log(msg);
        }

        // Value change
        if (!moduleDb.getValue().equals(value)) {
            moduleDb.setValue(value);
            log.atFine().log("Module value was updated: %s", moduleDb);
        }

        return moduleDb;
    }

    private Module createAndPersistNewModule(Long unitId, String moduleType, String name, Double value) {
        var module = new Module()
                .setUnitId(unitId)
                .setModule(moduleType)
                .setName(name)
                .setValue(value)
                .setActive(true);
        var moduleSaved = moduleRepository.saveAndFlush(module);

        var newModuleMessage = String.format("New Module was registered: %s", moduleSaved);

        var unitLog = new UnitLog()
                .setUnitId(unitId)
                .setTimeUtc(ZonedDateTime.now(ZoneOffset.UTC))
                .setType(UnitLog.Type.STATUS_CHANGE)
                .setLogEntry(newModuleMessage);
        unitLogRepository.save(unitLog);

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