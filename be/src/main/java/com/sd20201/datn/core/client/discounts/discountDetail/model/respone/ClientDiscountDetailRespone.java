package com.sd20201.datn.core.client.discounts.discountDetail.model.respone;

import java.math.BigDecimal;

public interface ClientDiscountDetailRespone {
    String getId();

    String getProductCode();

    String getProductDetailCode();

    String getImage();

    String getProductName();

    String getDiscountName();

    String getDiscountCode();

    Integer getPercentageDiscount();

    BigDecimal getPrice();

    Long getStartTime();

    Long getEndTime();

    String getDescription();

    String getColorName();

    String getRamName();

    String getHardDriveName();

    String getMaterialName();

    String getGpuName();

    String getCpuName();
}
