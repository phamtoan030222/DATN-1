package com.sd20201.datn.core.client.discounts.discount.model.response;

public interface ClientDiscountResponse {
    String getId();
    String getDiscountName();
    String getDiscountCode();
    Long getCreatedDate();
    Long getStartTime();
    Long getEndTime();
    Integer getPercentage();
    String getDescription();
    Long getProductCount();
}
