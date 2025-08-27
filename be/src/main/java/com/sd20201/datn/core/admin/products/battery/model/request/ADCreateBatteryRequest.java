package com.sd20201.datn.core.admin.products.battery.model.request;

import com.sd20201.datn.infrastructure.constant.TechnolyCharging;
import com.sd20201.datn.infrastructure.constant.TypeBattery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADCreateBatteryRequest {
    private String code;
    private String name;
    private String brand;
    private TechnolyCharging technolyCharging;
    private Integer capacity;
    private Boolean removeBattery;
    private TypeBattery type;
}
