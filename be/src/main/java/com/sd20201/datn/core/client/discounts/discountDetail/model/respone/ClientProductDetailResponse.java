package com.sd20201.datn.core.client.discounts.discountDetail.model.respone;

import java.math.BigDecimal;

public interface ClientProductDetailResponse {
    String getId();

    String getProductCode();

    String getProductDetailCode();

    String getImage();

    String getProductName();

    String getColorName();

    String getRamName();

    String getHardDriveName();

    String getMaterialName();

    String getGpuName();

    String getCpuName();

    BigDecimal getPrice();

    String getDescription();
}
