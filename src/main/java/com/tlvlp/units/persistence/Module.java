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
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true, fluent = true)
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "modules", catalog = "tlvlp_iot")
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Min(1L)
    @Column(name = "unit_id", nullable = false)
    public Long unitId;

    @NotBlank
    public String module;

    @NotBlank
    public String name;

    @EqualsAndHashCode.Exclude
    @NotNull
    public Double value;

    @EqualsAndHashCode.Exclude
    @NotNull
    private Boolean active;

}
