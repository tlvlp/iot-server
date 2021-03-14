package com.tlvlp.iot.server.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

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
        T saved = save(entity);
        flush();
        return saved;
    }

}
