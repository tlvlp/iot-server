package com.tlvlp.units.persistence;


import io.quarkus.hibernate.orm.panache.PanacheEntity;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

/**
 * A Microcontroller Unit (MCU)
 */
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true, fluent = true)
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "units", catalog = "tlvlp_iot")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String project;

    @NotBlank
    private String name;

    @EqualsAndHashCode.Exclude
    @NotNull
    private Boolean active;

    @EqualsAndHashCode.Exclude
    @NotNull
    @Column(name = "last_seen", nullable = false)
    private ZonedDateTime lastSeen;

    @EqualsAndHashCode.Exclude
    @NotBlank
    @Column(name = "control_topic", nullable = false)
    private String controlTopic;


}