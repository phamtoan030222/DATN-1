package com.sd20201.datn.core.client.discounts.discountDetail.model.request;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ClientCreateDiscountMultiRequest {

    @NotNull(message = "Vui lòng chọn sản phẩm")
    private List<String> productDetailIds;

    @NotNull(message = "Vui lòng chọn đợt giảm giá")
    private String discountId;

    @NotNull(message = "Giá gốc không được bỏ trống")
    private BigDecimal originalPrice;

    @NotNull(message = "Giá giảm không được bỏ trống")
    private BigDecimal salePrice;

    private String description;
}
