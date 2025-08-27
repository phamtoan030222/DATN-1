package com.sd20201.datn.core.admin.products.color.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColorCreateUpdateRequest {

    @NotBlank(message="Tên màu sắc không được để trống")
    private String colorName;

    @NotBlank(message="Vui lòng chọn mã màu")
    private String colorCode;
}
