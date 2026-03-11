package com.sd20201.datn.core.admin.hoadon.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADCapNhatKhachHangRequest {
    private String maHoaDon;
    private String tenKhachHang;
    private String sdtKH;
    private String email;
    private String diaChi;
}