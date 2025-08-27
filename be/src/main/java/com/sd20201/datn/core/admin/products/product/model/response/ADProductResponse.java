package com.sd20201.datn.core.admin.products.product.model.response;

import com.sd20201.datn.core.common.base.IsIdentify;

public interface ADProductResponse extends IsIdentify {

    String getCode();

    String getName();

    String getBrand();

    String getBattery();

    String getOperatingSystem();

    String getScreen();

    Long getCreatedDate();

}
