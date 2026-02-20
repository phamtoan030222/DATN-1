package com.sd20201.datn.core.admin.hoadon.repository;

import com.sd20201.datn.core.admin.hoadon.model.response.ADHoaDonChiTietResponseDetail;
import com.sd20201.datn.entity.InvoiceDetail;
import com.sd20201.datn.repository.InvoiceDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ADHoaDonChiTietRepository extends InvoiceDetailRepository {

    /**
     * Lấy chi tiết hóa đơn (POS + ONLINE)
     * - IMEI CHỈ xuất hiện khi hóa đơn >= ĐÃ XÁC NHẬN
     */
    @Query(value = """
    SELECT
        hd.id AS invoiceId,
        hd.code AS maHoaDon,
        hd.name AS tenHoaDon,
        hd.type_invoice AS loaiHoaDon,

        hdct.id AS id,
        hdct.code AS maHoaDonChiTiet,
        hdct.quantity AS soLuong,
        hdct.price AS giaBan,
        (hdct.price * hdct.quantity) AS tongTien,
        hd.total_amount_after_decrease AS tongTienSauGiam,
        v.code AS maVoucher,
        v.name AS tenVoucher,
        (hd.total_amount - hd.total_amount_after_decrease) AS giaTriVoucher, 
        s.name AS tenNhanVien,

        sp.name AS tenSanPham,
        spct.id AS productDetailId,
        spct.url_image AS anhSanPham,
        brand.name AS thuongHieu,
        color.name AS mauSac,
        CONCAT(ram.name, 'GB - ', hard_drive.name, 'GB') AS size,

        hd.trang_thai_hoa_don AS trangThaiHoaDon,
        hd.created_date AS ngayTao,
        hd.shipping_fee AS phiVanChuyen,
        hd.description AS phuongThucThanhToan,

        -- Tổng tiền hóa đơn
        (
            SELECT SUM(hdsub.quantity * hdsub.price)
            FROM invoice_detail hdsub
            WHERE hdsub.id_invoice = hd.id
        ) AS thanhTien,

        -- Thông tin khách hàng
        kh.name AS tenKhachHang,
        kh.phone AS sdtKH,
        kh.email AS email,
        CONCAT(
            COALESCE(addr.address_detail, ''),
            ', ',
            COALESCE(addr.ward_commune, ''),
            ', ',
            COALESCE(addr.district, ''),
            ', ',
            COALESCE(addr.province_city, '')
        ) AS diaChi,

        -- Lịch sử trạng thái
        (
            SELECT JSON_ARRAYAGG(
                JSON_OBJECT(
                    'id', lst.id,
                    'trangThai', lst.trang_thai,
                    'tenTrangThai',
                        CASE lst.trang_thai
                            WHEN 0 THEN 'Chờ xác nhận'
                            WHEN 1 THEN 'Đã xác nhận'
                            WHEN 2 THEN 'Chờ giao hàng'
                            WHEN 3 THEN 'Đang giao hàng'
                            WHEN 4 THEN 'Hoàn thành'
                            WHEN 5 THEN 'Đã hủy'
                            ELSE 'Không xác định'
                        END,
                    'thoiGian', lst.thoi_gian,
                    'ghiChu', lst.note,
                    'nhanVien', nv.name
                )
            )
            FROM lich_su_trang_thai_hoa_don lst
            LEFT JOIN staff nv ON lst.nhan_vien_id = nv.id
            WHERE lst.hoa_don_id = hd.id
        ) AS lichSuTrangThai,

        -- ===== IMEI (CHỈ SAU KHI XÁC NHẬN) =====
        (
            SELECT JSON_ARRAYAGG(
                JSON_OBJECT(
                    'id', i.id,
                    'code', i.code,
                    'status', i.trang_thai_imei,
                    'statusText',
                        CASE i.trang_thai_imei
                            WHEN 2 THEN 'Đã bán'
                            WHEN 1 THEN 'Đã đặt'
                            WHEN 4 THEN 'Đang bảo hành'
                            ELSE 'Không xác định'
                        END
                )
            )
            FROM imei i
            WHERE i.id_invoice_detail = hdct.id
        ) AS danhSachImei,

        (
            SELECT COUNT(*)
            FROM imei i
            WHERE i.id_invoice_detail = hdct.id
        ) AS soLuongImei
    

    FROM invoice hd
    LEFT JOIN invoice_detail hdct ON hdct.id_invoice = hd.id
    LEFT JOIN product_detail spct ON hdct.id_product_detail = spct.id
    LEFT JOIN product sp ON spct.id_product = sp.id
    LEFT JOIN brand brand ON sp.id_brand = brand.id
    LEFT JOIN color color ON spct.id_color = color.id
    LEFT JOIN ram ram ON spct.id_ram = ram.id
    LEFT JOIN hard_drive hard_drive ON spct.id_hard_drive = hard_drive.id
    LEFT JOIN customer kh ON hd.id_customer = kh.id
    LEFT JOIN address addr ON addr.id_customer = kh.id AND addr.is_default = TRUE
    LEFT JOIN voucher v ON hd.id_voucher = v.id
    LEFT JOIN staff s ON hd.id_staff = s.id

    WHERE hd.code = :maHoaDon
      AND (
            -- CHƯA XÁC NHẬN → HIỂN THỊ HẾT
            hd.trang_thai_hoa_don = 0

            -- ĐÃ XÁC NHẬN → CHỈ LẤY DÒNG CÓ IMEI
            OR EXISTS (
                SELECT 1
                FROM imei i
                WHERE i.id_invoice_detail = hdct.id
            )
      )

    ORDER BY hd.created_date DESC, hdct.created_date
    """,
            countQuery = """
        SELECT COUNT(*)
        FROM invoice_detail hdct
        LEFT JOIN invoice hd ON hdct.id_invoice = hd.id
        WHERE hd.code = :maHoaDon
          AND (
                hd.trang_thai_hoa_don = 0
                OR EXISTS (
                    SELECT 1
                    FROM imei i
                    WHERE i.id_invoice_detail = hdct.id
                )
          )
    """,
            nativeQuery = true
    )
    Page<ADHoaDonChiTietResponseDetail> getHoaDonChiTiet(
            @Param("maHoaDon") String maHoaDon,
            Pageable pageable
    );


    /**
     * Dùng cho service xác nhận đơn → gán IMEI
     */
    List<InvoiceDetail> findByInvoiceId(String invoiceId);
}
