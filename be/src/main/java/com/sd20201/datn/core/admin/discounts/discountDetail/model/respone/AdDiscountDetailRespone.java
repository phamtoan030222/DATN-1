package com.sd20201.datn.core.admin.discounts.discountDetail.model.respone;

import java.math.BigDecimal;

public interface AdDiscountDetailRespone {
    String getId();

    String getProductCode();

    String getProductName();

    String getDiscountName();

    String getDiscountCode();

    Integer getPercentageDiscount();

    BigDecimal getPrice();

    Long getStartTime();

    Long getEndTime();

    String getDescription();
}
