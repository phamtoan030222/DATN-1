package com.sd20201.datn.infrastructure.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum RoleConstant {

    QUAN_LY("Quản Lý"),

    NHAN_VIEN("Nhân Viên"),

    KHACH_HANG("Khách hàng"),
    ;

    private final String nameInVietnamese;

    RoleConstant(String nameInVietnamese) {
        this.nameInVietnamese = nameInVietnamese;
    }

    public static List<String> getAllRoles() {
        return Arrays.stream(RoleConstant.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public static String getAllRolesString() {
        return Arrays.stream(RoleConstant.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    public static List<String> getAllRolesInVietnamese() {
        return Arrays.stream(RoleConstant.values())
                .map(RoleConstant::getNameInVietnamese)
                .collect(Collectors.toList());
    }

    // lấy tên tiếng việt từng vai trò
    public static String getVietnameseNameByRole(String roleName) {
        return Arrays.stream(RoleConstant.values())
                .filter(role -> role.name().equalsIgnoreCase(roleName))
                .map(RoleConstant::getNameInVietnamese)
                .findFirst()
                .orElse(null);
    }

}
