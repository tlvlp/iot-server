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
import java.util.Set;
import java.util.logging.Level;
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

    @Transactional
    public void sendControlMessages(Long unitId, Collection<ModuleDTO> moduleControls) {
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
    }

    @ConsumeEvent("mqtt_ingress")
    @Transactional
    public Uni<Void> handleIngressMessage(Message message) {
        return Uni.createFrom().item(() -> {
            log.at(Level.FINE).log("Message event received: %s", message);
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
            return null;
        });
    }

    private void handleErrorMessage(JsonObject body) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);
        var unit = getOrCreateUnitFromBody(body)
                .active(true)
                .lastSeen(timeUtc);
        if(unit.id() == null) {
            unitRepository.save(unit);
            unitRepository.flush();
        }

        var error = body.getString("error");
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
        if(unit.lastSeen() == null) {
            // Keep last seen data if present.
            unit.lastSeen(ZonedDateTime.now(ZoneOffset.UTC));
        }
        unitRepository.save(unit);
        unitRepository.flush();

        var unitLog = new UnitLog()
                .unitId(unit.id())
                .time(timeUtc)
                .type(UnitLog.Type.INCOMING_INACTIVE)
                .logEntry(body.getString("error"));
        unitLogRepository.save(unitLog);

        eventBus.publish("unit_inactive", unit);
    }

    private void handleStatusMessage(JsonObject body) {
        var unit = getOrCreateUnitFromBody(body)
                .active(true)
                .lastSeen(ZonedDateTime.now(ZoneOffset.UTC));
        var savedUnit = unitRepository.save(unit);
        unitRepository.flush();

        if (unit.id() == null) {
            log.atInfo().log("Added a new Unit: %s", savedUnit);
        }

        var newModules = getOrCreateModulesFromBody(savedUnit, body);

        newModules.forEach(module -> {
            module.active(true);
            var savedModule = moduleRepository.save(module);
            if (module.id() == null) {
                log.atInfo().log("Added a new Module: %s", savedModule);
            }
        });

        moduleRepository.getAllActiveModulesByUnitId(unit.id()).stream()
                .filter(module -> !newModules.contains(module))
                .forEach(module -> {
                    module.active(false);
                    moduleRepository.save(module);
                    log.atInfo().log("A previously active Module is now missing from the Unit status. " +
                            "Marking module as inactive: %s", module);
                });
    }

    private Unit getOrCreateUnitFromBody(JsonObject body) {
        var idJson = body.getJsonObject("id");
        var project = idJson.getString("project");
        var name = idJson.getString("unitName");
        var unitOpt = unitRepository.findByProjectAndName(project, name);
        return unitOpt.orElseGet(() -> getNewUnit(project, name));
    }

    private Unit getNewUnit(String project, String name) {
        return new Unit()
            .project(project)
            .name(name)
            .controlTopic(generateControlTopic(project, name));
    }


    private String generateControlTopic(String project, String name) {
        return String.format("/units/%s-%s/control", project, name);
    }

    private Set<Module> getOrCreateModulesFromBody(Unit unit, JsonObject body) {
        return body.getJsonArray("modules")
                .stream()
                .map(moduleDtoObj -> Json.decodeValue(String.valueOf(moduleDtoObj), ModuleDTO.class))
                .map(moduleDTO -> getOrCreateModule(unit.id(), moduleDTO))
                .collect(Collectors.toSet());
    }

    private Module getOrCreateModule(Long unitId, ModuleDTO dto) {
        var module = dto.module();
        var name = dto.name();
        var value = dto.value();
        var moduleDb = moduleRepository.findByUnitIdAndModuleAndName(unitId, module, name)
                .orElseGet(() ->  new Module()
                        .unitId(unitId)
                        .module(module)
                        .name(name)
                        .value(value)
                        .active(true));
        if(!moduleDb.value().equals(value)) {
            moduleDb.value(value);
            log.atInfo().log("Module value has been updated: %s", moduleDb);
        }
        return moduleDb;

    }

}
