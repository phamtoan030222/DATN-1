package com.sd20201.datn.core.admin.staff.model.request;

import lombok.Getter;
import lombok.Setter;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class ADCreateStaff {

    // Account info
    @NotBlank
    private String fullName;

    @NotBlank
    private String role; // string từ combobox, sẽ convert sang RoleConstant

    // Staff info
    private String citizenIdentifyCard;
    private Long birthday; // timestamp, có thể đổi sang LocalDate nếu muốn
    private String hometown;
    private Boolean gender;

    @Email
    private String email;

    private String phone;
    private String avatarUrl;
    private String provinceCode;
    private String districtCode;
    private String communeCode;

}

