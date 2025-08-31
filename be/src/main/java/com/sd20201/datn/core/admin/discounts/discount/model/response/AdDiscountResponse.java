package com.sd20201.datn.core.admin.discounts.discount.model.response;

public interface AdDiscountResponse {
    String getId();
    String getDiscountName();
    String getDiscountCode();
    Long getCreatedDate();
    Long getStartTime();
    Long getEndTime();
    Integer getPercentage();
    String getDescription();
}
