package com.tlvlp.units;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class UnitRepository implements PanacheRepository<Unit> {

    public Optional<Unit> findByProjectAndName(String project, String name) {
        return find("project = ?1 and name = ?2", project, name)
                .firstResultOptional();
    }

}
