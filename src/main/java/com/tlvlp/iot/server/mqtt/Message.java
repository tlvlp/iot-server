package com.tlvlp.iot.server.mqtt;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

@RegisterForReflection
@Getter
@Setter
@ToString
@Accessors(chain = true, fluent = true)
@NoArgsConstructor
public class Message implements Serializable {

    private String topic;
    private JsonObject payload;

}
