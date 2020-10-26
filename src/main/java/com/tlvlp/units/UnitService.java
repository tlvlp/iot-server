package com.tlvlp.units;

import com.tlvlp.mqtt.GlobalTopics;
import com.tlvlp.mqtt.Message;
import com.tlvlp.mqtt.MessageService;
import com.tlvlp.units.persistence.Module;
import com.tlvlp.units.persistence.ModuleDTO;
import com.tlvlp.units.persistence.ModuleRepository;
import com.tlvlp.units.persistence.Unit;
import com.tlvlp.units.persistence.UnitLog;
import com.tlvlp.units.persistence.UnitLogRepository;
import com.tlvlp.units.persistence.UnitRepository;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import lombok.extern.flogger.Flogger;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Startup
@Flogger
@ApplicationScoped
public class UnitService {

    private final EventBus eventBus;
    private final MessageService messageService;
    private final UnitRepository unitRepository;
    private final ModuleRepository moduleRepository;
    private final UnitLogRepository unitLogRepository;

    public UnitService(EventBus eventBus,
                       MessageService messageService,
                       UnitRepository unitRepository,
                       ModuleRepository moduleRepository,
                       UnitLogRepository unitLogRepository) {
        this.eventBus = eventBus;
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

    public Uni<Void> sendControlMessages(Long unitId, Collection<ModuleDTO> moduleControls) {
        try {
            Unit unit = unitRepository.findByIdOptional(unitId)
                    .orElseThrow(() -> new UnitException("Cannot find unit by Id:" + unitId));
            var body = Json.encodeToBuffer(moduleControls);
            messageService.sendMessage(unit.controlTopic(), body);

            var unitLog = new UnitLog()
                    .unitId(unit.id())
                    .time(ZonedDateTime.now(ZoneOffset.UTC))
                    .type(UnitLog.Type.OUTGOING_CONTROL)
                    .logEntry(Json.encodePrettily(moduleControls));
            unitLogRepository.save(unitLog);

            return Uni.createFrom().voidItem();
        } catch (Exception e) {
            return Uni.createFrom().failure(e);
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
                .active(true)
                .lastSeen(timeUtc);

        var error = Optional.ofNullable(body.getString("error"))
                .orElseGet(() -> {
                    log.atSevere().log("Missing error message for unit: %s", unit);
                    return "Error message is missing!";
                });
        var unitLog = new UnitLog()
                .unitId(unit.id())
                .time(timeUtc)
                .type(UnitLog.Type.INCOMING_ERROR)
                .logEntry(error);
        unitLogRepository.save(unitLog);

        eventBus.publish("unit_error", Map.of(
                "unit", unit,
                "error", error));
    }

    private void handleInactiveMessage(JsonObject body) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);
        var unit = getOrCreateUnitFromBody(body)
                .active(false);
        if (unit.lastSeen() == null) {
            // Keep last seen data if present.
            unit.lastSeen(ZonedDateTime.now(ZoneOffset.UTC));
        }
        var unitSaved = unitRepository.saveAndFlush(unit);

        var unitLog = new UnitLog()
                .unitId(unitSaved.id())
                .time(timeUtc)
                .type(UnitLog.Type.INCOMING_INACTIVE)
                .logEntry("Unit is inactive.");
        unitLogRepository.save(unitLog);

        eventBus.publish("unit_inactive", unitSaved);
    }

    private void handleStatusMessage(JsonObject body) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);
        var unit = getOrCreateUnitFromBody(body)
                .active(true)
                .lastSeen(timeUtc);
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
                .project(project)
                .name(name)
                .active(true)
                .lastSeen(timeUtc)
                .controlTopic(generateControlTopic(project, name));
        var unitSaved = unitRepository.saveAndFlush(unit);

        var unitLog = new UnitLog()
                .unitId(unitSaved.id())
                .time(timeUtc)
                .type(UnitLog.Type.STATUS_CHANGE)
                .logEntry("New Unit was registered.");
        unitLogRepository.save(unitLog);

        log.atInfo().log("New Unit was registered: %s", unitSaved);

        return unitSaved;
    }

    private String generateControlTopic(String project, String name) {
        return String.format("/units/%s-%s/control", project, name);
    }

    private void updateOrCreateModulesFromBody(Unit unit, JsonObject body) {
        var newModules = body.getJsonArray("modules")
                .stream()
                .map(moduleDtoObj -> Json.decodeValue(String.valueOf(moduleDtoObj), ModuleDTO.class))
                .map(moduleDTO -> updateOrCreateModule(unit.id(), moduleDTO))
                .collect(Collectors.toSet());

        // Check for modules that are active in the DB but are no longer present in the status summary and inactivate them.
        moduleRepository.getAllActiveModulesByUnitId(unit.id())
                .stream()
                .filter(module -> !newModules.contains(module))
                .forEach(module -> {
                    module.active(false);
                    moduleRepository.save(module);

                    var updateMessage = String.format("Module was inactivated: %s", module);

                    log.atInfo().log(updateMessage);

                    var unitLog = new UnitLog()
                            .unitId(unit.id())
                            .time(ZonedDateTime.now(ZoneOffset.UTC))
                            .type(UnitLog.Type.STATUS_CHANGE)
                            .logEntry(updateMessage);
                    unitLogRepository.save(unitLog);
                });
    }

    private Module updateOrCreateModule(Long unitId, ModuleDTO dto) {
        var moduleType = dto.module();
        var name = dto.name();
        var value = dto.value();

        var moduleDb = moduleRepository.findByUnitIdAndModuleAndName(unitId, moduleType, name)
                .orElseGet(() -> createAndPersistNewModule(unitId, moduleType, name, value));

        // Reactivation
        if (!moduleDb.active().equals(true)) {
            moduleDb.active(true);
            var msg = String.format("Module was reactivated: %s", moduleDb);
            var unitLog = new UnitLog()
                    .unitId(unitId)
                    .time(ZonedDateTime.now(ZoneOffset.UTC))
                    .type(UnitLog.Type.STATUS_CHANGE)
                    .logEntry(msg);
            unitLogRepository.save(unitLog);
            log.atInfo().log(msg);
        }

        // Value change
        if (!moduleDb.value().equals(value)) {
            moduleDb.value(value);
            log.atFine().log("Module value was updated: %s", moduleDb);
        }

        return moduleDb;
    }

    private Module createAndPersistNewModule(Long unitId, String moduleType, String name, Double value) {
        var module = new Module()
                .unitId(unitId)
                .module(moduleType)
                .name(name)
                .value(value)
                .active(true);
        var moduleSaved = moduleRepository.saveAndFlush(module);

        var newModuleMessage = String.format("New Module was registered: %s", moduleSaved);

        var unitLog = new UnitLog()
                .unitId(unitId)
                .time(ZonedDateTime.now(ZoneOffset.UTC))
                .type(UnitLog.Type.STATUS_CHANGE)
                .logEntry(newModuleMessage);
        unitLogRepository.save(unitLog);

        log.atInfo().log(newModuleMessage);

        return moduleSaved;
    }

}