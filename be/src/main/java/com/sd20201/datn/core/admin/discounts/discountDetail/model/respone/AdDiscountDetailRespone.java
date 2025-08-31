package com.sd20201.datn.core.admin.discounts.discountDetail.model.respone;

public interface AdDiscountDetailRespone {
    String getId();

    String getProductCode();

    String getProductName();

    String getDiscountName();

    String getDiscountCode();

    Integer getPercentageDiscount();

    Long getStartTime();

    Long getEndTime();

    String getDescription();
}
