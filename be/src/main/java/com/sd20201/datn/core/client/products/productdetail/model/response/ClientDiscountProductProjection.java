package com.sd20201.datn.core.client.products.productdetail.model.response;

import java.math.BigDecimal;

public interface ClientDiscountProductProjection {
    // Thông tin đợt giảm giá
    String getDiscountName();

    Long getStartDate();

    Long getEndDate();

    Integer getPercentage();

    // Thông tin sản phẩm chi tiết
    String getProductName();

    String getProductDetailId();

    // Cấu hình
    String getCpu();

    String getGpu();

    String getRam();

    String getHardDrive();

    String getUrlImage();

    // Giá
    BigDecimal getOriginalPrice();

    BigDecimal getSalePrice();
}
