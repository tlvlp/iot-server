package com.tlvlp.iot.server.controllers;

import com.tlvlp.iot.server.mcu.Mcu;
import com.tlvlp.iot.server.scheduler.SchedulerService;
import com.tlvlp.iot.server.mcu.Module;
import com.tlvlp.iot.server.mcu.McuLog;
import com.tlvlp.iot.server.mcu.McuService;
import io.smallrye.common.annotation.Blocking;
import lombok.extern.flogger.Flogger;

import javax.transaction.Transactional;
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

    @Blocking
    @Transactional
    @GET
    @Path("/all")
    public List<Mcu> getAllMcus() {
        return mcuService.getAllMcus();
    }

    @Blocking
    @Transactional
    @GET
    public Mcu getMcuById(@QueryParam("mcu_id") @NotNull @Min(1L) Long mcuId) {
        return mcuService.getMcuById(mcuId);
    }


    @Blocking
    @Transactional
    @GET
    @Path("/logs")
    public List<McuLog> getMcuLogsByMcuId(@QueryParam("mcu_id") @NotNull @Min(1L) Long mcuId) {
        return mcuService.getMcuLogsByMcuId(mcuId);
    }

    @Blocking
    @Transactional
    @GET
    @Path("/modules")
    public List<Module> getModulesByMcuId(@QueryParam("mcu_id") @NotNull @Min(1L) Long mcuId) {
        return mcuService.getModulesByMcuId(mcuId);
    }

    @Blocking
    @Transactional
    @POST
    @Path("/control")
    public void sendControlMessages(@NotEmpty List<Module> moduleControls) {
        mcuService.sendControlMessages(moduleControls);
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
