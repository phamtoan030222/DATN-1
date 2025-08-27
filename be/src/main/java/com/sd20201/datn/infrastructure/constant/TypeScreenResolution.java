package com.sd20201.datn.infrastructure.constant;

import lombok.Getter;

@Getter
public enum TypeScreenResolution {

    FULL_HD("1920x1080"),
    HD("1366x768"),
    RETINA("2880x1800"),
    UHD_4K("3840x2160"),
    QHD("2560x1440");

    private final String code;

    TypeScreenResolution(String code) {
        this.code = code;
    }

    public static TypeScreenResolution getByCode(String code) {
        for (TypeScreenResolution resolution : values()) {
            if (resolution.code.equals(code)) {
                return resolution;
            }
        }
        return null;
    }
}
