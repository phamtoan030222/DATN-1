package com.sd20201.datn.core.admin.hoadon.model.response;


import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.TypeInvoice;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ADHoaDonResponse {
    private String id;
    private String maHoaDon;
    private String tenKhachHang;
    private String sdtKhachHang;
    private String maNhanVien;
    private String tenNhanVien;
    private BigDecimal tongTien;
    private TypeInvoice loaiHoaDon;
    private Long createdDate;
    private EntityTrangThaiHoaDon status;
}