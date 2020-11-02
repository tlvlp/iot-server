package com.tlvlp.iot.server.controllers;

import com.tlvlp.iot.server.scheduler.EventJob;
import com.tlvlp.iot.server.scheduler.ScheduledEventException;
import com.tlvlp.iot.server.scheduler.SchedulerService;
import com.tlvlp.iot.server.units.UnitService;
import com.tlvlp.iot.server.units.Module;
import com.tlvlp.iot.server.units.ModuleDTO;
import com.tlvlp.iot.server.units.Unit;
import com.tlvlp.iot.server.units.UnitLog;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.extern.flogger.Flogger;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.TriggerBuilder;

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
import java.util.Collection;
import java.util.List;

import static org.quartz.CronScheduleBuilder.cronSchedule;

@Flogger
@Path("/units")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UnitController {

    private final UnitService unitService;
    private final SchedulerService schedulerService;

    public UnitController(UnitService unitService, SchedulerService schedulerService) {
        this.unitService = unitService;
        this.schedulerService = schedulerService;
    }

    @GET
    @Path("/all")
    public Multi<Unit> getAllUnits() {
        return unitService.getAllUnits();
    }

    @GET
    @Path("/{unit_id}")
    public Uni<Unit> getUnitById(@PathParam("unit_id") @NotNull @Min(1L) Long unitId) {
        return unitService.getUnitById(unitId);
    }

    @GET
    @Path("/{unit_id}/logs")
    public Multi<UnitLog> getUnitLogsByUnitId(@PathParam("unit_id") @NotNull @Min(1L) Long unitId) {
        return unitService.getUnitLogsByUnitId(unitId);
    }

    @GET
    @Path("/{unit_id}/modules")
    public Multi<Module> getModulesByUnitId(@PathParam("unit_id") @NotNull @Min(1L) Long unitId) {
        return unitService.getModulesByUnitId(unitId);
    }

    @POST
    @Path("/control")
    public Uni<Void> sendControlMessages(@NotEmpty List<Module> moduleControls) {
        return unitService.sendControlMessages(moduleControls);
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
