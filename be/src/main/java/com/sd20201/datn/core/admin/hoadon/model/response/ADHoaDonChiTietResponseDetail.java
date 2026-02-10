package com.sd20201.datn.core.admin.hoadon.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ADHoaDonChiTietResponseDetail {
    // Thông tin hóa đơn
    String getMaHoaDon();
    String getTenHoaDon();
    String getMaHoaDonChiTiet();

    String getInvoiceId();

    // Thông tin sản phẩm
    String getTenSanPham();
    String getAnhSanPham();
    String getThuongHieu();
    String getXuatSu();
    String getMauSac();
    String getSize();
    Integer getSoLuong();
    BigDecimal getGiaBan(); // Thay Double bằng BigDecimal

    // Thông tin giá trị
    BigDecimal getThanhTienSP(); // Thay Double bằng BigDecimal
    BigDecimal getThanhTien(); // Thay Double bằng BigDecimal

    // Thông tin khách hàng (từ invoice)
    String getTenKhachHang();
    String getSdtKH();
    String getEmail();
    String getDiaChi();

    // Thông tin khách hàng (từ customer entity)
    String getTenKhachHang2();
    String getSdtKH2();
    String getEmail2();
    String getDiaChi2();

    // Thông tin hóa đơn
    String getLoaiHoaDon(); // TypeInvoice enum
    String getTrangThaiHoaDon(); // EntityTrangThaiHoaDon enum
    LocalDateTime getThoiGian();
    Long getNgayTao();
    BigDecimal getPhiVanChuyen(); // Thay Double bằng BigDecimal

    // Thông tin voucher
    String getMaVoucher();
    String getTenVoucher();
    BigDecimal getGiaTriVoucher(); // Thay Double bằng BigDecimal
    BigDecimal getTongTienSauGiam(); // Thay Double bằng BigDecimal
    BigDecimal getTongTien(); // Thay Double bằng BigDecimal

    // Thông tin thanh toán (không có trong entity Invoice)
    String getPhuongThucThanhToan(); // Cần bổ sung vào entity

    // Thông tin nợ và hoàn phí (từ LichSuThanhToan)
    BigDecimal getDuNo(); // Thay Double bằng BigDecimal
    BigDecimal getHoanPhi(); // Thay Double bằng BigDecimal

    // Thêm các field mới
    String getLichSuTrangThai(); // JSON string
    String getTrangThaiText();
    LocalDateTime getThoiGianCapNhatCuoi();

    List<String> getDanhSachImei();

    Integer getSoLuongImei();

    String getProductDetailId();

}