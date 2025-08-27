package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.TechnolyCharging;
import com.sd20201.datn.infrastructure.constant.TypeBattery;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "battery")
public class Battery extends PrimaryEntity implements Serializable {

    private String brand;

    @Enumerated(EnumType.ORDINAL)
    private TechnolyCharging technolyCharging;

    private Integer capacity;

    private Boolean removeBattery;

    @Enumerated(EnumType.ORDINAL)
    private TypeBattery typeBattery;
}
