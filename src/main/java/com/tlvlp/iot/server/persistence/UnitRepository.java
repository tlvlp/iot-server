package com.tlvlp.iot.server.persistence;

import com.tlvlp.iot.server.units.Unit;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class UnitRepository implements PanacheRepositoryWithSave<Unit> {

    public Optional<Unit> findByProjectAndName(String project, String name) {
        return find("project = ?1 and name = ?2", project, name)
                .singleResultOptional();
    }

}
