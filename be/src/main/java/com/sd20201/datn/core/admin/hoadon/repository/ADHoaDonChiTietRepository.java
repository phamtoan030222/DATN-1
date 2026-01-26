package com.sd20201.datn.core.admin.hoadon.repository;

import com.sd20201.datn.core.admin.hoadon.model.response.ADHoaDonChiTietResponseDetail;
import com.sd20201.datn.repository.InvoiceDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ADHoaDonChiTietRepository extends InvoiceDetailRepository {

    @Query(value = """
            SELECT
                hd.code AS maHoaDon,
                hd.name AS tenHoaDon,
                hdct.code AS maHoaDonChiTiet,
                sp.name AS tenSanPham,
                spct.url_image AS anhSanPham,
                brand.name AS thuongHieu,
                color.name AS mauSac,
                CONCAT(ram.name, 'GB - ', hard_drive.name, 'GB') AS size,
                hdct.quantity AS soLuong,
                hdct.price AS giaBan,
                (
                    SELECT SUM(hdsub.quantity * hdsub.price)
                    FROM invoice_detail hdsub
                    WHERE hdsub.id_invoice = hd.id
                ) AS thanhTien,
                kh.name AS tenKhachHang2,
                kh.phone AS sdtKH2,
                kh.email AS email2,
                CONCAT(
                    COALESCE(addr.address_detail, ''),
                    ', ',
                    COALESCE(addr.ward_commune, ''),
                    ', ',
                    COALESCE(addr.district, ''),
                    ', ',
                    COALESCE(addr.province_city, '')
                ) AS diaChi2,                               
                hd.name_receiver AS tenKhachHang,
                hd.phone_receiver AS sdtKH,
                kh.email AS email,
                hd.address_receiver AS diaChi,
                hd.type_invoice AS loaiHoaDon,
                hd.trang_thai_hoa_don AS trangThaiHoaDon,
                hd.created_date AS ngayTao,
                hd.shipping_fee AS phiVanChuyen,
                pgg.code AS maVoucher,
                pgg.name AS tenVoucher,
                hd.total_amount - hd.total_amount_after_decrease AS giaTriVoucher,
                hd.total_amount_after_decrease AS tongTienSauGiam,
                (hdct.price * hdct.quantity) AS tongTien,
                hd.description AS phuongThucThanhToan
            FROM invoice hd
            LEFT JOIN invoice_detail hdct ON hdct.id_invoice = hd.id
            LEFT JOIN voucher pgg ON hd.id_voucher = pgg.id
            LEFT JOIN customer kh ON hd.id_customer = kh.id
            LEFT JOIN address addr ON addr.id_customer = kh.id AND addr.is_default = TRUE
            LEFT JOIN product_detail spct ON hdct.id_product_detail = spct.id
            LEFT JOIN product sp ON spct.id_product = sp.id
            LEFT JOIN brand brand ON sp.id_brand = brand.id
            LEFT JOIN color color ON spct.id_color = color.id
            LEFT JOIN ram ram ON spct.id_ram = ram.id
            LEFT JOIN hard_drive hard_drive ON spct.id_hard_drive = hard_drive.id
            WHERE hd.code = :maHoaDon
        """,
            countQuery = """
                SELECT COUNT(*)
                FROM invoice_detail hdct
                LEFT JOIN invoice hd ON hdct.invoice_id = hd.id
                WHERE hd.code = :maHoaDon
                """,
            nativeQuery = true)
    Page<ADHoaDonChiTietResponseDetail> getAllHoaDonChiTietResponse(@Param("maHoaDon") String maHoaDon, Pageable pageable);
    @Query(value = """
                SELECT
                    hd.ma_hoa_don AS maHoaDon,
                    hd.ten_hoa_don AS tenHoaDon,
                    hdct.ma_hoa_don_chi_tiet AS maHoaDonChiTiet,
                    sp.ten_san_pham AS tenSanPham,
                    spct.url_image AS anhSanPham,
                    brand.ten_thuong_hieu AS thuongHieu,
                    color.ten_mau_sac AS mauSac,
                    CONCAT(ram.dung_luong, 'GB - ', hard_drive.dung_luong, 'GB') AS size,
                    hdct.quantity AS soLuong,
                    hdct.price AS giaBan,
                    (
                        SELECT SUM(hdsub.quantity * hdsub.price)
                        FROM invoice_detail hdsub
                        WHERE hdsub.id_invoice = hd.id
                    ) AS thanhTien,
                    hd.name_receiver AS tenKhachHang,
                    hd.phone_receiver AS sdtKH,
                    kh.email AS email,
                    hd.address_receiver AS diaChi,
                    hd.type_invoice AS loaiHoaDon,
                    hd.trang_thai_hoa_don AS trangThaiHoaDon,
                    hd.created_date AS ngayTao,
                    hd.shipping_fee AS phiVanChuyen,
                    pgg.ma_voucher AS maVoucher,
                    pgg.ten_voucher AS tenVoucher,
                    pgg.discount_value AS giaTriVoucher,
                    hd.total_amount_after_decrease AS tongTienSauGiam,
                    (hdct.price * hdct.quantity) AS tongTien,
                    hd.description as phuongThucThanhToan,
                    (
                        SELECT COALESCE(SUM(lstt.so_tien), 0)
                        FROM lich_su_thanh_toan lstt
                        WHERE lstt.hoa_don_id = hd.id
                    ) AS duNo,
                    (
                        SELECT COALESCE(SUM(lstt.so_tien), 0)
                        FROM lich_su_thanh_toan lstt
                        WHERE lstt.hoa_don_id = hd.id AND lstt.loai_giao_dich = 'HOAN_TIEN'
                    ) AS hoanPhi
                FROM invoice_detail hdct
                LEFT JOIN invoice hd ON hdct.invoice_id = hd.id
                LEFT JOIN voucher pgg ON hd.voucher_id = pgg.id
                LEFT JOIN customer kh ON hd.customer_id = kh.id
                LEFT JOIN product_detail spct ON hdct.product_detail_id = spct.id
                LEFT JOIN product sp ON spct.product_id = sp.id
                LEFT JOIN brand ON sp.brand_id = brand.id
                LEFT JOIN color ON spct.color_id = color.id
                LEFT JOIN ram ON spct.ram_id = ram.id
                LEFT JOIN hard_drive ON spct.hard_drive_id = hard_drive.id
                WHERE hd.ma_hoa_don = :maHoaDon
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM invoice_detail hdct
                    LEFT JOIN invoice hd ON hdct.invoice_id = hd.id
                    WHERE hd.ma_hoa_don = :maHoaDon
                    """,
            nativeQuery = true)
    List<ADHoaDonChiTietResponseDetail> getAllHoaDonChiTietResponse2(@Param("maHoaDon") String maHoaDon);

    @Query(value = """
            SELECT
                hd.ma_hoa_don AS maHoaDon,
                hd.ten_hoa_don AS tenHoaDon,
                hdct.ma_hoa_don_chi_tiet AS maHoaDonChiTiet,
                sp.ten_san_pham AS tenSanPham,
                spct.url_image AS anhSanPham,
                brand.ten_thuong_hieu AS thuongHieu,
                color.ten_mau_sac AS mauSac,
                CONCAT(ram.dung_luong, 'GB - ', hard_drive.dung_luong, 'GB') AS size,
                hdct.quantity AS soLuong,
                spct.price AS giaBan,
                (hdct.quantity * spct.price) AS thanhTienSP,
                (SELECT SUM(hdct.quantity * spct.price) 
                 FROM invoice_detail hdct 
                 JOIN product_detail spct ON spct.id = hdct.product_detail_id  
                 WHERE hdct.invoice_id = hd.id) AS thanhTien,
                kh.ten_khach_hang AS tenKhachHang,
                kh.so_dien_thoai AS sdtKH,
                kh.email AS email,
                kh.dia_chi AS diaChi,
                hd.type_invoice AS loaiHoaDon,
                hd.trang_thai_hoa_don AS trangThaiHoaDon,
                hd.created_date AS ngayTao,
                hd.shipping_fee AS phiVanChuyen,
                pgg.ma_voucher AS maVoucher,
                hd.total_amount_after_decrease AS tongTienSauGiam,
                hd.total_amount AS tongTien
            FROM invoice_detail hdct
            LEFT JOIN invoice hd ON hdct.invoice_id = hd.id
            LEFT JOIN voucher pgg ON hd.voucher_id = pgg.id
            LEFT JOIN customer kh ON hd.customer_id = kh.id
            LEFT JOIN product_detail spct ON hdct.product_detail_id = spct.id
            LEFT JOIN product sp ON spct.product_id = sp.id
            LEFT JOIN brand ON sp.brand_id = brand.id
            LEFT JOIN color ON spct.color_id = color.id
            LEFT JOIN ram ON spct.ram_id = ram.id
            LEFT JOIN hard_drive ON spct.hard_drive_id = hard_drive.id
            WHERE hd.ma_hoa_don = :maHoaDon
            """, nativeQuery = true)
    List<ADHoaDonChiTietResponseDetail> getAllHoaDonChiTietResponse1(@Param("maHoaDon") String maHoaDon);
}