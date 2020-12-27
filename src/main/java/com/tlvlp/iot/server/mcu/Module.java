package com.tlvlp.iot.server.mcu;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
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

    @NotNull
    @Min(1L)
    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @NotBlank
    private String module;

    @NotBlank
    private String name;

    @EqualsAndHashCode.Exclude
    @NotNull
    private Double value;

    @EqualsAndHashCode.Exclude
    @NotNull
    private Boolean active;

}
