package com.tlvlp.iot.server.persistence;

import com.tlvlp.iot.server.mcu.Module;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class ModuleRepository implements PanacheRepositoryWithSave<Module> {

    public Set<Module> getAllActiveModulesByMcuId(Long mcuId) {
        return new HashSet<>(list("mcu_id = ?1 and active = ?2", mcuId, true));
    }

    public Optional<Module> findByMcuIdAndModuleAndName(Long mcuId, String module, String name) {
        return find("mcu_id = ?1 and module = ?2 and name = ?3", mcuId, module, name)
                .singleResultOptional();
    }

    public List<Module> findAllByMcuId(Long mcuId) {
        return list("mcu_id", mcuId);
    }
}
