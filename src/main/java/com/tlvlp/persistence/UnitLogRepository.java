package com.tlvlp.persistence;

import com.tlvlp.units.UnitLog;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class UnitLogRepository implements PanacheRepositoryWithSave<UnitLog> {

    public List<UnitLog> findAllByUnitId(Long unitId) {
        return list("id", unitId);
    }
}
