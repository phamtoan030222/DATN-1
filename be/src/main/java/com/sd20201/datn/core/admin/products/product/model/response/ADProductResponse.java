package com.sd20201.datn.core.admin.products.product.model.response;

import com.sd20201.datn.core.common.base.IsEntityStatus;
import com.sd20201.datn.core.common.base.IsIdentify;

public interface ADProductResponse extends IsIdentify, IsEntityStatus {

    String getCode();

    String getName();

    String getBrand();

    String getBattery();

    String getOperatingSystem();

    String getScreen();

    Integer getMinPrice();

    Integer getMaxPrice();

    Integer getQuantity();

    Long getCreatedDate();

    String getUrlImage();

    Integer getPercentage();
}
