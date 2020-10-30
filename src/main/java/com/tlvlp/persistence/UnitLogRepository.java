package com.tlvlp.persistence;

import com.tlvlp.units.UnitLog;
import io.quarkus.panache.common.Sort;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class UnitLogRepository implements PanacheRepositoryWithSave<UnitLog> {

    public List<UnitLog> findAllByUnitId(Long unitId) {
        return list("unit_id", Sort.ascending("id"), unitId);
    }
}
