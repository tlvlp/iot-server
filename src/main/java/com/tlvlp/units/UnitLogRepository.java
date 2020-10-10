package com.tlvlp.units;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class UnitLogRepository implements PanacheRepository<UnitLog> {

}
