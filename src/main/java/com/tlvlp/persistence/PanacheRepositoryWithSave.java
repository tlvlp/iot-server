package com.tlvlp.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

public interface PanacheRepositoryWithSave<T> extends PanacheRepository<T> {

    default T save(T entity) {
        if(isPersistent(entity)) {
            persist(entity);
            return entity;
        } else {
            return getEntityManager().merge(entity);
        }
    }

    default T saveAndFlush(T entity) {
        var saved = save(entity);
        flush();
        return saved;
    }

}
