package com.sd20201.datn.core.client.banhang.model.response;

import java.math.BigDecimal;

public interface ClientGioHangResponse {
    Long getSTT();
    String getIdHDCT();
    String getId();
    String getTen();
    Integer getSoLuong();
    BigDecimal getGiaBan();
    String getAnh();
    String getStatus();
    String getCpu();
    String getRam();
    String getHardDrive();
    String getGpu();
    String getColor();
    String getImel();
}
