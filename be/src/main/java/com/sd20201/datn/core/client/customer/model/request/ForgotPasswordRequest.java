package com.sd20201.datn.core.client.customer.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {
    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;
}