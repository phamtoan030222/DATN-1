package com.sd20201.datn.core.client.banhang.model.response;

import java.math.BigDecimal;

public interface ClientGioHangResponse {
    Long getSTT();

    String getIdHDCT();

    String getId();

    String getTen();

    Integer getSoLuong();

    // Giá gốc (giá niêm yết)
    BigDecimal getGiaBan();

    // Giá sau khi giảm (nếu có đợt giảm giá)
    BigDecimal getGiaSauGiam();

    String getAnh();

    // Các thông số kỹ thuật
    String getCpu();

    String getRam();

    String getHardDrive();

    String getGpu();

    String getColor();

    String getImel(); // Danh sách IMEI (nếu group_concat)
}