package com.sd20201.datn.core.admin.discounts.discount.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiscountValidateRequest {

    @NotBlank(message = "Tên Đợt giảm giá không được để trống")
    private String discountName;

    @NotBlank(message = "Mã Đợt giảm giá không được để trống")
    private String discountCode;

    @NotNull(message = "Đơn vị giảm không được để trống")
    private Integer percentage;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private Long startDate;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private Long endDate;

    private String description;

}
