package com.tlvlp.controllers;

import com.tlvlp.units.UnitService;
import com.tlvlp.units.Module;
import com.tlvlp.units.ModuleDTO;
import com.tlvlp.units.Unit;
import com.tlvlp.units.UnitLog;
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
import java.util.Collection;

@Flogger
@Path("/units")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
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
    @Path("/{unit_id}/control")
    public Uni<Void> sendControlMessages(@PathParam("unit_id") @NotNull @Min(1L) Long unitId,
                                         @NotEmpty Collection<ModuleDTO> moduleControls) {
        return unitService.sendControlMessages(unitId, moduleControls);
    }
}
