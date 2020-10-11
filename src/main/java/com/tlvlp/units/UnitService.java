package com.tlvlp.units;

import com.tlvlp.mqtt.GlobalTopics;
import com.tlvlp.mqtt.Message;
import com.tlvlp.mqtt.MessageService;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.buffer.Buffer;
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
@Transactional
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
        unitLogRepository.persistAndFlush(unitLog);
    }

    @ConsumeEvent("mqtt_ingress")
    public void handleIngressMessage(Message message) {
        log.at(Level.FINER).log("Message event received: %s", message);
        var body = message.payload();
        var topic = message.topic();
        if (topic.equals(GlobalTopics.GLOBAL_STATUS.topic())) {
            handleStatusMessage(body);
        }
        else if (topic.equals(GlobalTopics.GLOBAL_INACTIVE.topic())) {
            handleInactiveMessage(body);
        }
        else if (topic.equals(GlobalTopics.GLOBAL_ERROR.topic())) {
            handleErrorMessage(body);
        } else {
            log.atSevere().log("Unrecognized topic name: %s", topic);
        }
    }

    private void handleErrorMessage(JsonObject body) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);
        var unit = getUnitFromBody(body)
                .active(true)
                .lastSeen(timeUtc);
        if(unit.id() == null) {
            unitRepository.persistAndFlush(unit);
        }

        var error = body.getString("error");
        var unitLog = new UnitLog()
                .unitId(unit.id())
                .time(timeUtc)
                .type(UnitLog.Type.INCOMING_ERROR)
                .logEntry(error);
        unitLogRepository.persistAndFlush(unitLog);

        eventBus.publish("unit_error", Map.of(
                "unit", unit,
                "error", error));
    }

    private void handleInactiveMessage(JsonObject body) {
        var timeUtc = ZonedDateTime.now(ZoneOffset.UTC);
        var unit = getUnitFromBody(body)
                .active(false);
        if(unit.lastSeen() == null) {
            // Keep last seen data if present.
            unit.lastSeen(ZonedDateTime.now(ZoneOffset.UTC));
        }
        unitRepository.persistAndFlush(unit);

        var unitLog = new UnitLog()
                .unitId(unit.id())
                .time(timeUtc)
                .type(UnitLog.Type.INCOMING_INACTIVE)
                .logEntry(body.getString("error"));
        unitLogRepository.persistAndFlush(unitLog);

        eventBus.publish("unit_inactive", unit);
    }

    private void handleStatusMessage(JsonObject body) {
        var unit = getUnitFromBody(body)
                .active(true)
                .lastSeen(ZonedDateTime.now(ZoneOffset.UTC));
        unitRepository.persistAndFlush(unit);

        getOrCreateModulesFromBody(unit, body)
                .forEach(moduleRepository::persist);
    }

    private Unit getUnitFromBody(JsonObject body) {
        var idObject = body.getJsonObject("id");
        var project = idObject.getString("project");
        var name = idObject.getString("unitName");
        var unitOpt = unitRepository.findByProjectAndName(project, name);
        return unitOpt.orElse(getNewUnit(project, name));
    }

    private Unit getNewUnit(String project, String name) {
        log.atInfo().log("Creating new Unit with: project=%s, name=%s", project, name);
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
                .map(moduleDtoObj -> Json.decodeValue((Buffer)moduleDtoObj, ModuleDTO.class))
                .map(moduleDTO -> getOrCreateModule(unit.id(), moduleDTO))
                .collect(Collectors.toSet());
    }

    private Module getOrCreateModule(Long unitId, ModuleDTO dto) {
        var module = dto.module();
        var name = dto.name();
        var value = dto.value();
        var moduleDb = moduleRepository.findByUnitIdAndModuleAndName(unitId, module, name)
                .orElseGet(() -> {
                    var newModule = new Module()
                                    .unitId(unitId)
                                    .module(module)
                                    .name(name)
                                    .value(value);
                    log.atInfo().log("Module has been created: %s", newModule);
                    return newModule;
                });
        if(!moduleDb.value().equals(value)) {
            moduleDb.value(value);
            log.atFine().log("Module value has been updated: %s", moduleDb);
        }
        return moduleDb;

    }


}
