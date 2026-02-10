package com.sd20201.datn.core.client.banhang.model.response;

import java.math.BigDecimal;

public interface ClientBanHangProductDetailCartResponse {

    String getId();

    String getName();

    Integer getQuantity();

    BigDecimal getPrice();

    BigDecimal getPercentage();

    String getImageUrl();

    String getCpu();

    String getRam();

    String getHardDrive();

    String getGpu();

    String getColor();

    String getMaterial();
}
