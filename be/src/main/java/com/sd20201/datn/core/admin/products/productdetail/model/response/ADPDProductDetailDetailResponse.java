package com.sd20201.datn.core.admin.products.productdetail.model.response;

import com.sd20201.datn.core.common.base.IsIdentify;

public interface ADPDProductDetailDetailResponse extends IsIdentify {

    String getCode();

    String getName();

    String getDescription();

    String getIdProduct();

    String getIdCPU();

    Double getPrice();

    String getIdGPU();

    String getIdColor();

    String getIdRAM();

    String getIdHardDrive();

    String getIdMaterial();
}
