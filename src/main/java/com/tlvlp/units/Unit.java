package com.tlvlp.units;


import io.quarkus.hibernate.orm.panache.PanacheEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A Microcontroller Unit (MCU)
 */
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "units", catalog = "tlvlp_iot")
public class Unit extends PanacheEntity {

    @NotBlank
    public String project;

    @NotBlank
    public String name;

    @EqualsAndHashCode.Exclude
    @NotNull
    public Boolean active;

    @EqualsAndHashCode.Exclude
    @NotNull
    @Column(name = "last_seen", nullable = false)
    public ZonedDateTime lastSeen;

    @EqualsAndHashCode.Exclude
    @NotBlank
    @Column(name = "control_topic", nullable = false)
    public String controlTopic;


}