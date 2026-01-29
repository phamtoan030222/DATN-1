package com.sd20201.datn.core.client.discounts.discountDetail.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ClientCreateDiscountRequest {

    @NotNull(message = "Vui lòng chọn sản phẩm")
    private String productDetailId;

    @NotNull(message = "Vui lòng chọn đợt giảm giá")
    private String discountId;

    @NotNull(message = "Giá gốc không được bỏ trống")
    private BigDecimal originalPrice;

    @NotNull(message = "Giá giảm không được bỏ trống")
    private BigDecimal salePrice;

    private String description;

}
