package com.sd20201.datn.core.client.banhang.model.response;

import java.math.BigDecimal;

public interface ClientCartItemResponse {

    String getId();

    String getName();

    String getProductDetailId();

    Integer getQuantity();

    BigDecimal getPrice();

    BigDecimal getPercentage();

    String getImageUrl();

    // Các thông số kỹ thuật
    String getCpu();

    String getRam();

    String getHardDrive();

    String getGpu();

    String getColor();

    String getMaterial();
}