package com.tlvlp.units.persistence;

import com.tlvlp.persistence.PanacheRepositoryWithSave;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class UnitRepository implements PanacheRepositoryWithSave<Unit> {

    public Optional<Unit> findByProjectAndName(String project, String name) {
        return find("project = ?1 and name = ?2", project, name)
                .singleResultOptional();
    }

}
