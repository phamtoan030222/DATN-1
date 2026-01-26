package com.sd20201.datn.core.admin.hoadon.model.request;

import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import lombok.Data;

@Data
public class ADChangeStatusRequest {

    private String maHoaDon;

    private EntityTrangThaiHoaDon status;

    private String note;
}