package com.tlvlp.iot.server.persistence;

import com.tlvlp.iot.server.mcu.Mcu;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class McuRepository implements PanacheRepositoryWithSave<Mcu> {

    public Optional<Mcu> findByProjectAndName(String project, String name) {
        return find("project = ?1 and name = ?2", project, name)
                .singleResultOptional();
    }

}
