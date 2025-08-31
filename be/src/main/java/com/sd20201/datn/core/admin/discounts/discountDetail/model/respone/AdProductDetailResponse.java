package com.sd20201.datn.core.admin.discounts.discountDetail.model.respone;

import java.math.BigDecimal;

public interface AdProductDetailResponse {
    String getId();

    String getProductCode();

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
