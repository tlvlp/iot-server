package com.tlvlp.mqtt;

import java.util.Arrays;
import java.util.stream.Stream;

public enum GlobalTopics {

    GLOBAL_STATUS_REQUEST("/global/status_request"),
    GLOBAL_STATUS("/global/status"),
    GLOBAL_INACTIVE("/global/inactive"),
    GLOBAL_ERROR("/global/error");

    private final String topic;

    GlobalTopics(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }

    public static Stream<String> getAll() {
        return Arrays.stream(GlobalTopics.values())
                .map(GlobalTopics::topic);

    }
}
