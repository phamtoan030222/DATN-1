package com.sd20201.datn.core.admin.banhang.model.response;

import java.math.BigDecimal;

public interface ADGioHangResponse {
    Long getSTT();
    String getIdHDCT();
    String getId();
    String getTen();
    Integer getSoLuong();
    BigDecimal getgiaGoc();
    BigDecimal getGiaBan();
    String getAnh();
    String getStatus();
    String getCpu();
    String getRam();
    String getHardDrive();
    String getGpu();
    String getColor();
    String getImel();
    Integer getPercentage();
}
