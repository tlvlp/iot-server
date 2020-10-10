package com.tlvlp.units;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class ModuleRepository implements PanacheRepository<Module> {

    public Optional<Module> findByUnitIdAndModuleAndName(Long unitId, String module, String name) {
        return find("unit_id = ?1 and module = ?2 and name =?3", unitId, module, name)
                .firstResultOptional();
    }

}
