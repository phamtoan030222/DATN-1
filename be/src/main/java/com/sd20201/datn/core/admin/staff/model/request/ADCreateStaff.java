package com.sd20201.datn.core.admin.staff.model.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class ADCreateStaff {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Chức vụ không được để trống")
    private String role;

    // Staff info
    @NotBlank(message = "CCCD/CMND không được để trống") // Nên bắt buộc
    private String citizenIdentifyCard;

    private Long birthday;
    private String hometown;
    private Boolean gender;

    @NotBlank(message = "Email không được để trống") // Nên có NotBlank kèm Email
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống") // Nên bắt buộc
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$", message = "SĐT không hợp lệ") // Validate số VN
    private String phone;

    private String avatarUrl;

    // --- ĐỊA CHỈ ---
    @NotBlank(message = "Tỉnh/Thành phố không được để trống") // Bắt buộc
    private String provinceCode;

    // DISTRICT CODE: GIỮ NGUYÊN KHÔNG VALIDATE (Để tương thích API v2)
    private String districtCode;

    @NotBlank(message = "Phường/Xã không được để trống") // Bắt buộc
    private String communeCode;

}

