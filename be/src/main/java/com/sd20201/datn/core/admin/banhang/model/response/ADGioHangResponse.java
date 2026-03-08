package com.sd20201.datn.core.admin.banhang.model.response;

import java.math.BigDecimal;

public interface ADGioHangResponse {

    Integer getStt();

    String getIdHDCT();

    String getId();

    String getTen();

    Integer getSoLuong();

    BigDecimal getGiaGoc();

    BigDecimal getGiaBan();

    BigDecimal getGiaHienTai();

    Integer getGiaDaThayDoi();

    String getAnh();

    String getCpu();

    String getRam();

    String getHardDrive();

    String getGpu();

    String getColor();

    String getImel();

    Integer getPercentage();

    String getStatus();
}