package com.tlvlp.iot.server.mcu;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class ModuleDTO implements Serializable {

    @NotBlank
    public String module;

    @NotBlank
    public String name;

    @NotBlank
    public String action;

    @NotNull
    public Double value;

}
