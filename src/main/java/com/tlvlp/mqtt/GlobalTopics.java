package com.tlvlp.mqtt;

import java.util.Arrays;
import java.util.stream.Stream;

public enum GlobalTopics {

    GLOBAL_STATUS_REQUEST("/global/status_request", false),
    GLOBAL_STATUS("/global/status", true),
    GLOBAL_INACTIVE("/global/inactive", true),
    GLOBAL_ERROR("/global/error", true);

    private final String topic;
    private final Boolean isIngress;

    GlobalTopics(String topic, Boolean isIngress) {
        this.topic = topic;
        this.isIngress = isIngress;
    }

    public String topic() {
        return topic;
    }

    public Boolean isIngress() {
        return isIngress;
    }

    public static Stream<String> getIngressTopicStream() {
        return Arrays.stream(GlobalTopics.values())
                .filter(GlobalTopics::isIngress)
                .map(GlobalTopics::topic);

    }
}
