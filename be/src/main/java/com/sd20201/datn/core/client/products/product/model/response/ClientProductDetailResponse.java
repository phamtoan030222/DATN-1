package com.sd20201.datn.core.client.products.product.model.response;

import com.sd20201.datn.core.common.base.IsIdentify;

public interface ClientProductDetailResponse extends IsIdentify {

    String getCode();

    String getName();

    String getIdBrand();

    String getIdBattery();

    String getIdOperatingSystem();

    String getIdScreen();

}
