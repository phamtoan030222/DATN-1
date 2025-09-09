package com.sd20201.datn.core.admin.customer.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CustomerCreateUpdateRequest {
    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(min = 2, max = 100, message = "Tên khách hàng phải từ 2-100 ký tự")
    @Pattern(regexp = "^[a-zA-ZÀ-ỹ\\s0-9]+$", message = "Tên khách hàng không được chứa ký tự đặc biệt")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(03|05|07|08|09)[0-9]{8,9}$", message = "Số điện thoại không hợp lệ")
    private String customerPhone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được quá 255 ký tự")
    private String customerEmail;

    @Size(max = 500, message = "URL avatar không được quá 500 ký tự")
    private String customerAvatar;

    // 🔄 Chuyển thành optional vì khách hàng có thể chưa có tài khoản
    private String customerIdAccount;

    @JsonIgnore
    private String customerCode;

    private String customerDescription;

    private Integer customerStatus;

    @NotNull(message = "Giới tính không được để trống")
    private Boolean customerGender; // false = Nữ, true = Nam

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private Long customerBirthday; // timestamp (milliseconds)

}
