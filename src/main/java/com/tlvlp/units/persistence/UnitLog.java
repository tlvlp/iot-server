package com.tlvlp.units.persistence;


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
import java.time.ZonedDateTime;

/**
 * A log entry related to a Microcontroller Unit (MCU).
 */
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true, fluent = true)
@ToString
@Entity
@Table(name ="unit_logs", catalog = "tlvlp_iot")
public class UnitLog {

    public enum Type {
        INCOMING_ERROR, INCOMING_INACTIVE, OUTGOING_CONTROL, STATUS_CHANGE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Min(1L)
    @Column(name = "unit_id", nullable = false)
    public Long unitId;

    @NotNull
    public ZonedDateTime time;

    @NotNull
    @Enumerated(EnumType.STRING)
    public Type type;

    @NotBlank
    @Column(name = "log_entry", nullable = false)
    public String logEntry;

}