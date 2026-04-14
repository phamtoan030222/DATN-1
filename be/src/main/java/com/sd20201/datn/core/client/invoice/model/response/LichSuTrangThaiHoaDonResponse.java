package com.sd20201.datn.core.client.invoice.model.response;

import java.time.LocalDateTime;

public interface LichSuTrangThaiHoaDonResponse {

    String getId();

    String getNote();

    Integer getTrangThai();

    LocalDateTime getThoiGian();

    String getIdStaff();

    String getNameStaff();

    String getIdCustomer();

    String getNameCustomer();

}
