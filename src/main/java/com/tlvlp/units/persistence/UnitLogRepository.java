package com.tlvlp.units.persistence;

import com.tlvlp.persistence.PanacheRepositoryWithSave;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class UnitLogRepository implements PanacheRepositoryWithSave<UnitLog> {

    public List<UnitLog> findAllByUnitId(Long unitId) {
        return list("id", unitId);
    }
}
