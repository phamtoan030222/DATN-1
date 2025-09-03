package com.sd20201.datn.core.admin.customer.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CustomerCreateUpdateRequest {
    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String customerPhone;

    @Email(message = "Email không hợp lệ")
    private String customerEmail;

    private String customerAvatar;

    @NotBlank(message = "Tài khoản khách hàng không được để trống")
    private String customerIdAccount;

    private String customerCode;

    private String customerDescription;

    private Integer customerStatus;

    @NotNull(message = "Giới tính không được để trống")
    private Boolean customerGender; // 0 = Nữ, 1 = Nam (ví dụ)

    private Long customerBirthday; // lưu dưới dạng timestamp (milliseconds)

}
