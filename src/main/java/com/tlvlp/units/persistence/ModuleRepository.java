package com.tlvlp.units.persistence;

import com.tlvlp.persistence.PanacheRepositoryWithSave;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ModuleRepository implements PanacheRepositoryWithSave<Module> {

    public Set<Module> getAllActiveModulesByUnitId(Long unitId) {
        return find("unit_id = ?1 and active = ?2", unitId, true)
                .stream()
                .collect(Collectors.toSet());
    }

    public Optional<Module> findByUnitIdAndModuleAndName(Long unitId, String module, String name) {
        return find("unit_id = ?1 and module = ?2 and name = ?3", unitId, module, name)
                .singleResultOptional();
    }

}
