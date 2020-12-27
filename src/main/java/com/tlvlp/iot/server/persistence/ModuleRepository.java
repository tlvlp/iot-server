package com.tlvlp.iot.server.persistence;

import com.tlvlp.iot.server.mcu.Module;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class ModuleRepository implements PanacheRepositoryWithSave<Module> {

    public Set<Module> getAllActiveModulesByUnitId(Long unitId) {
        return new HashSet<>(list("unit_id = ?1 and active = ?2", unitId, true));
    }

    public Optional<Module> findByUnitIdAndModuleAndName(Long unitId, String module, String name) {
        return find("unit_id = ?1 and module = ?2 and name = ?3", unitId, module, name)
                .singleResultOptional();
    }

    public List<Module> findAllByUnitId(Long unitId) {
        return list("unit_id", unitId);
    }
}
