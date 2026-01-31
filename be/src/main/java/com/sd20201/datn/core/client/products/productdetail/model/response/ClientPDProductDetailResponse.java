package com.sd20201.datn.core.client.products.productdetail.model.response;

import com.sd20201.datn.core.common.base.IsEntityStatus;
import com.sd20201.datn.core.common.base.IsIdentify;
import lombok.Getter;
import lombok.Setter;


public interface ClientPDProductDetailResponse extends IsIdentify, IsEntityStatus {

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

    Integer getQuantity();

    String getUrlImage();

    Integer getPercentage();

    String getProductName();


    // Nếu bạn muốn lấy thêm màn hình, hãng, pin...
    String getScreenName();

    String getBrandName();

    String getBatteryName();

    String getOperatingSystemName();

}
