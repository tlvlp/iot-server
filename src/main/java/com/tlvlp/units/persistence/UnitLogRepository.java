package com.tlvlp.units.persistence;

import com.tlvlp.persistence.PanacheRepositoryWithSave;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UnitLogRepository implements PanacheRepositoryWithSave<UnitLog> {

}
