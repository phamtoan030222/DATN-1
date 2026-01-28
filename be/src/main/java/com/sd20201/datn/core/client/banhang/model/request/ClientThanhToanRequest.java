package com.sd20201.datn.core.client.banhang.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClientThanhToanRequest {

    private String idHD; // ID Hóa đơn
    private String idNV; // Có thể null

    // Thông tin khách hàng
    private String ten;
    private String sdt;
    private String diaChi;
    private String ghiChu;

    // Tiền nong (Dùng BigDecimal để chính xác)
    private BigDecimal tongTien; // Tổng thanh toán cuối cùng
    private BigDecimal tienHang; // Tổng tiền hàng (subTotal)
    private BigDecimal tienShip;
    private BigDecimal giamGia;

    // Các thông tin khác
    private String phuongThucThanhToan; // "0": Tiền mặt/COD, "1": CK
    private String idPGG; // ID Voucher
    private String loaiHoaDon;
    private Integer check; // 0 hoặc 1
}

