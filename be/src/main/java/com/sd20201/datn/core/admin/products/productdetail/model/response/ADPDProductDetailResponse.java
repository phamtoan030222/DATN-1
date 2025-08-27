package com.sd20201.datn.core.admin.products.productdetail.model.response;

import com.sd20201.datn.core.common.base.IsIdentify;

public interface ADPDProductDetailResponse extends IsIdentify {

    String getCode();

    String getName();

    String getColor();

    String getRam();

    String getHardDrive();

    String getMaterial();

    String getCpu();

    String getGpu();

    Double getPrice();

    String getDescription();

}
