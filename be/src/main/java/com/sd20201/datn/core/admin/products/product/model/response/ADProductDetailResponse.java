package com.sd20201.datn.core.admin.products.product.model.response;

import com.sd20201.datn.core.common.base.IsIdentify;

public interface ADProductDetailResponse extends IsIdentify {

    String getCode();

    String getName();

    String getBrand();

    String getIdBattery();

    String getIdOperatingSystem();

    String getIdScreen();

}
