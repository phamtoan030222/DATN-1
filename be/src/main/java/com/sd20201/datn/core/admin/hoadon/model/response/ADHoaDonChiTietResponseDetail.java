package com.sd20201.datn.core.admin.hoadon.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ADHoaDonChiTietResponseDetail {
    String getId();
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
    BigDecimal getGiaBan();

    // Thông tin giá trị
    BigDecimal getThanhTienSP();
    BigDecimal getThanhTien();
    BigDecimal getTongTien(); // Thêm field này

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
    String getLoaiHoaDon();
    String getTrangThaiHoaDon();
    LocalDateTime getThoiGian();
    Long getNgayTao();
    BigDecimal getPhiVanChuyen();

    // Thông tin voucher
    String getMaVoucher();
    String getTenVoucher();
    BigDecimal getGiaTriVoucher();
    BigDecimal getTongTienSauGiam();

    // Thông tin thanh toán
    String getPhuongThucThanhToan();

    // Thông tin nợ và hoàn phí
    BigDecimal getDuNo();
    BigDecimal getHoanPhi();

    // Lịch sử và trạng thái
    String getLichSuTrangThai();
    String getTrangThaiText();
    LocalDateTime getThoiGianCapNhatCuoi();

    // IMEI
    String getDanhSachImei();  // Đổi từ List<String> thành String vì là JSON string
    Integer getSoLuongImei();

    // Các trường khác
    String getProductDetailId();
    String getTenNhanVien();
}