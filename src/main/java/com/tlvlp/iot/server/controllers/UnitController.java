package com.tlvlp.iot.server.controllers;

import com.tlvlp.iot.server.scheduler.SchedulerService;
import com.tlvlp.iot.server.mcu.Module;
import com.tlvlp.iot.server.mcu.Unit;
import com.tlvlp.iot.server.mcu.UnitLog;
import com.tlvlp.iot.server.mcu.McuService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.extern.flogger.Flogger;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Flogger
@Path("/units")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UnitController {

    private final McuService mcuService;
    private final SchedulerService schedulerService;

    public UnitController(McuService mcuService, SchedulerService schedulerService) {
        this.mcuService = mcuService;
        this.schedulerService = schedulerService;
    }

    @GET
    @Path("/all")
    public Multi<Unit> getAllUnits() {
        return mcuService.getAllUnits();
    }

    @GET
    @Path("/{unit_id}")
    public Uni<Unit> getUnitById(@PathParam("unit_id") @NotNull @Min(1L) Long unitId) {
        return mcuService.getUnitById(unitId);
    }

    @GET
    @Path("/{unit_id}/logs")
    public Multi<UnitLog> getUnitLogsByUnitId(@PathParam("unit_id") @NotNull @Min(1L) Long unitId) {
        return mcuService.getUnitLogsByUnitId(unitId);
    }

    @GET
    @Path("/{unit_id}/modules")
    public Multi<Module> getModulesByUnitId(@PathParam("unit_id") @NotNull @Min(1L) Long unitId) {
        return mcuService.getModulesByUnitId(unitId);
    }

    @POST
    @Path("/control")
    public Uni<Void> sendControlMessages(@NotEmpty List<Module> moduleControls) {
        return mcuService.sendControlMessages(moduleControls);
    }

    public void addScheduledEvent(String schedulerGroup, String schedulerName, String cron, String eventAddress, String eventMessage) {
        //TODO
    }

    public void pauseScheduledEvent() {
        //TODO
    }

    public void removeScheduledEvent() {
        //TODO
    }

    public void getAllScheduledEvents() {
        //TODO
    }

    public void getScheduledEventsForUnit(Long unitId) {
        //TODO
    }
}
