package com.tlvlp.iot.server.controllers;

import com.tlvlp.iot.server.mcu.Mcu;
import com.tlvlp.iot.server.scheduler.SchedulerService;
import com.tlvlp.iot.server.mcu.Module;
import com.tlvlp.iot.server.mcu.McuLog;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Flogger
@Path("/mcu")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class McuController {

    private final McuService mcuService;
    private final SchedulerService schedulerService;

    public McuController(McuService mcuService, SchedulerService schedulerService) {
        this.mcuService = mcuService;
        this.schedulerService = schedulerService;
    }

    @GET
    @Path("/all")
    public Multi<Mcu> getAllMcus() {
        return mcuService.getAllMcus();
    }

    @GET
    public Uni<Mcu> getMcuById(@QueryParam("mcu_id") @NotNull @Min(1L) Long mcuId) {
        return mcuService.getMcuById(mcuId);
    }

    @GET
    @Path("/logs")
    public Multi<McuLog> getMcuLogsByMcuId(@QueryParam("mcu_id") @NotNull @Min(1L) Long mcuId) {
        return mcuService.getMcuLogsByMcuId(mcuId);
    }

    @GET
    @Path("/modules")
    public Multi<Module> getModulesByMcuId(@QueryParam("mcu_id") @NotNull @Min(1L) Long mcuId) {
        return mcuService.getModulesByMcuId(mcuId);
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

    public void getScheduledEventsForMcu(Long mcuId) {
        //TODO
    }
}
