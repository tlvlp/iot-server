package com.tlvlp.iot.server.persistence;

import com.tlvlp.iot.server.mcu.McuLog;
import io.quarkus.panache.common.Sort;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class McuLogRepository implements PanacheRepositoryWithSave<McuLog> {

    public List<McuLog> findAllByMcuId(Long mcuId) {
        return list("mcu_id", Sort.ascending("id"), mcuId);
    }
}
