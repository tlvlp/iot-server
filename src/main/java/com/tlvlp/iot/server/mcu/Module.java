package com.tlvlp.iot.server.mcu;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * A Module for a Microcontroller Unit (MCU).
 * eg. a temperature sensor or a relay.
 */
@RegisterForReflection
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "modules", catalog = "tlvlp_iot")
public class Module implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The id of the MCU the module belongs to.
     */
    @NotNull
    @Min(1L)
    @Column(name = "mcu_id", nullable = false)
    private Long mcuId;

    /**
     * The type of the module.
     * eg. relay or DS18B20 temp sensor
     */
    @NotBlank
    private String module;

    /**
     * Groups the modules by action.
     * eg. 'read' for passive modules or 'switch' for different kinds of relays.
     * Keeping it as a String helps to kepp the back-end agnostic of the module implementations
     * on the MCU and front-end sides.
     */
    @NotBlank
    private String action;

    /**
     * Distinguishes the module within the MCU
     * eg. if there are two temperature sensors with the same module type and action type
     */
    @NotBlank
    private String name;

    /**
     * The current value of the module.
     */
    @EqualsAndHashCode.Exclude
    @NotNull
    private Double value;

    /**
     * true if the module is considered active.
     */
    @EqualsAndHashCode.Exclude
    @NotNull
    private Boolean active;

}
