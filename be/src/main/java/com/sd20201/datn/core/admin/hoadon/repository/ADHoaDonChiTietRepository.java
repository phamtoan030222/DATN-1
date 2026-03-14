package com.sd20201.datn.core.admin.hoadon.repository;

import com.sd20201.datn.core.admin.hoadon.model.response.ADHoaDonChiTietResponseDetail;
import com.sd20201.datn.entity.InvoiceDetail;
import com.sd20201.datn.repository.InvoiceDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ADHoaDonChiTietRepository extends InvoiceDetailRepository {

    @Query(value = """
SELECT
    hd.id                                    AS invoiceId,
    hd.code                                  AS maHoaDon,
    hd.name                                  AS tenHoaDon,
    hd.type_invoice                          AS loaiHoaDon,

    hdct.id                                  AS id,
    hdct.code                                AS maHoaDonChiTiet,
    hdct.quantity                            AS soLuong,

    -- ===== GIÁ =====
    hdct.gia_goc                             AS giaGoc,

    hdct.price                                     AS giaBan,

    CASE
        WHEN hdct.gia_goc IS NULL OR hdct.gia_goc = 0 THEN 0
        ELSE ROUND((hdct.gia_goc - hdct.price) * 100.0 / hdct.gia_goc, 2)
        END         AS percentage,

    CASE
        WHEN discount_info.max_percentage IS NOT NULL THEN 1
        ELSE 0
    END                                      AS giaDaThayDoi,

    -- ===== THÀNH TIỀN (tính theo giá sau giảm) =====
    (
        CASE
            WHEN discount_info.max_percentage IS NOT NULL
            THEN (hdct.price * (100 - discount_info.max_percentage)) / 100
            ELSE hdct.price
        END * hdct.quantity
    )                                        AS tongTien,

    hd.total_amount_after_decrease           AS tongTienSauGiam,

    -- ===== VOUCHER =====
    v.code                                   AS maVoucher,
    v.name                                   AS tenVoucher,
    COALESCE(hd.total_amount - hd.total_amount_after_decrease, 0) AS giaTriVoucher,

    -- ===== NHÂN VIÊN =====
    s.name                                   AS tenNhanVien,

    -- ===== SẢN PHẨM =====
    sp.name                                  AS tenSanPham,
    spct.id                                  AS productDetailId,
    spct.url_image                           AS anhSanPham,
    brand.name                               AS thuongHieu,
    color.name                               AS mauSac,
    CONCAT(
        COALESCE(ram.name, ''), 'GB - ',
        COALESCE(hard_drive.name, ''), 'GB'
    )                                        AS size,

    -- ===== TRẠNG THÁI & THỜI GIAN =====
    hd.trang_thai_hoa_don                    AS trangThaiHoaDon,
    hd.trang_thai_thanh_toan                 AS trangThaiThanhToan,
    hd.created_date                          AS ngayTao,
    hd.last_modified_date                    AS ngayCapNhat,
    COALESCE(hd.shipping_fee, 0)             AS phiVanChuyen,
    hd.da_hoan_phi AS trangThaihoanPhi,
    hd.hoan_phi AS hoanPhi,

    -- ===== PHƯƠNG THỨC THANH TOÁN =====
    CASE hd.phuong_thuc_thanh_toan
        WHEN 0 THEN 'TIEN_MAT'
        WHEN 1 THEN 'VNPAY'
        WHEN 2 THEN 'CHUYEN_KHOAN'
        WHEN 3 THEN 'THE_TIN_DUNG'
        WHEN 4 THEN 'VI_DIEN_TU'
        WHEN 5 THEN 'TIEN_MAT_CHUYEN_KHOAN'
        ELSE NULL
    END                                      AS phuongThucThanhToan,

    -- ===== TỔNG THÀNH TIỀN ĐƠN HÀNG =====
    (
        SELECT COALESCE(SUM(hdsub.quantity * hdsub.price), 0)
        FROM invoice_detail hdsub
        WHERE hdsub.id_invoice = hd.id
    )                                        AS thanhTien,

    -- ===== THÔNG TIN KHÁCH HÀNG =====
    COALESCE(hd.name_receiver, '')           AS tenKhachHang,
    COALESCE(hd.phone_receiver, '')          AS sdtKH,
    COALESCE(NULLIF(hd.email, ''), kh.email, '') AS email,
    kh.email                                 AS email2,
    COALESCE(hd.address_receiver, '')        AS diaChi,

    -- ===== LỊCH SỬ TRẠNG THÁI =====
    (
        SELECT JSON_ARRAYAGG(
            JSON_OBJECT(
                'id',          lst.id,
                'trangThai',   lst.trang_thai,
                'tenTrangThai',
                    CASE lst.trang_thai
                        WHEN 0 THEN 'Chờ xác nhận'
                        WHEN 1 THEN 'Đã xác nhận'
                        WHEN 2 THEN 'Chờ giao hàng'
                        WHEN 3 THEN 'Đang giao hàng'
                        WHEN 4 THEN 'Hoàn thành'
                        WHEN 5 THEN 'Đã hủy'
                        WHEN 6 THEN 'Sẵn sàng nhận hàng'
                        ELSE 'Không xác định'
                    END,
                'thoiGian',    lst.thoi_gian,
                'ghiChu',      COALESCE(lst.note, ''),
                'nhanVien',    COALESCE(nv.name, '')
            )
        )
        FROM lich_su_trang_thai_hoa_don lst
        LEFT JOIN staff nv ON lst.nhan_vien_id = nv.id
        WHERE lst.hoa_don_id = hd.id
    )                                        AS lichSuTrangThai,

    -- ===== DANH SÁCH IMEI =====
    COALESCE(
        (
            SELECT JSON_ARRAYAGG(
                JSON_OBJECT(
                    'id',         i.id,
                    'code',       i.code,
                    'status',     i.trang_thai_imei,
                    'statusText',
                        CASE i.trang_thai_imei
                            WHEN 0 THEN 'Khả dụng'
                            WHEN 1 THEN 'Đã đặt'
                            WHEN 2 THEN 'Đã bán'
                            WHEN 3 THEN 'Lỗi'
                            WHEN 4 THEN 'Đang bảo hành'
                            ELSE 'Không xác định'
                        END,
                    'assignedAt', i.created_date
                )
            )
            FROM imei i
            WHERE i.id_invoice_detail = hdct.id
        ),
        JSON_ARRAY()
    )                                        AS danhSachImei,

    -- ===== SỐ LƯỢNG IMEI =====
    (
        SELECT COUNT(*)
        FROM imei i
        WHERE i.id_invoice_detail = hdct.id
    )                                        AS soLuongImei

FROM invoice hd
INNER JOIN invoice_detail hdct   ON hdct.id_invoice        = hd.id
LEFT JOIN  product_detail spct   ON hdct.id_product_detail = spct.id
LEFT JOIN  product sp            ON spct.id_product        = sp.id
LEFT JOIN  brand brand           ON sp.id_brand            = brand.id
LEFT JOIN  color color           ON spct.id_color          = color.id
LEFT JOIN  ram ram               ON spct.id_ram            = ram.id
LEFT JOIN  hard_drive hard_drive ON spct.id_hard_drive     = hard_drive.id
LEFT JOIN  customer kh           ON hd.id_customer         = kh.id
LEFT JOIN  voucher v             ON hd.id_voucher          = v.id
LEFT JOIN  staff s               ON hd.id_staff            = s.id
LEFT JOIN (
    SELECT
        pdd.id_product_detail,
        MAX(d.percentage) AS max_percentage
    FROM product_detail_discount pdd
    JOIN discount d ON pdd.id_discount = d.id
    WHERE pdd.status = 0
      AND d.status   = 0
      AND d.start_date <= :currentTime
      AND d.end_date   >= :currentTime
    GROUP BY pdd.id_product_detail
) discount_info ON spct.id = discount_info.id_product_detail

WHERE hd.code = :maHoaDon

ORDER BY hdct.created_date ASC
""",
            countQuery = """
SELECT COUNT(*)
FROM invoice_detail hdct
LEFT JOIN invoice hd ON hdct.id_invoice = hd.id
WHERE hd.code = :maHoaDon
""",
            nativeQuery = true
    )
    Page<ADHoaDonChiTietResponseDetail> getHoaDonChiTiet(
            @Param("maHoaDon") String maHoaDon,
            @Param("currentTime") Long currentTime,
            Pageable pageable
    );

    /**
     * Dùng cho service xác nhận đơn → gán IMEI
     */
    List<InvoiceDetail> findByInvoiceId(String invoiceId);
}
