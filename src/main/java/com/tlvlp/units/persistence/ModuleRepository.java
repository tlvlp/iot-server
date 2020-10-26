package com.tlvlp.units.persistence;

import com.tlvlp.persistence.PanacheRepositoryWithSave;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
