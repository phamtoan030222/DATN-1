package com.sd20201.datn.infrastructure.constant;

import lombok.Getter;

@Getter
public enum TypeBattery {

    LI_ION("Li-ion"),
    LI_PO("Li-po"),
    ;

    private final String value;

    TypeBattery(String value) {
        this.value = value;
    }

}
