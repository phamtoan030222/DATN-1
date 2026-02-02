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
            hd.description AS phuongThucThanhToan,
            -- Thêm lịch sử trạng thái dạng JSON
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
                        'nhanVien', nv.name,
                        'maNhanVien', nv.code
                    )
                )
                FROM lich_su_trang_thai_hoa_don lst
                LEFT JOIN staff nv ON lst.nhan_vien_id = nv.id
                WHERE lst.hoa_don_id = hd.id
                ORDER BY lst.thoi_gian DESC
            ) AS lichSuTrangThai,
            -- Trạng thái hiện tại chi tiết
            (
                SELECT 
                    CASE hd.trang_thai_hoa_don
                        WHEN 0 THEN 'Chờ xác nhận'
                        WHEN 1 THEN 'Đã xác nhận'
                        WHEN 2 THEN 'Chờ giao hàng'
                        WHEN 3 THEN 'Đang giao hàng'
                        WHEN 4 THEN 'Hoàn thành'
                        WHEN 5 THEN 'Đã hủy'
                        ELSE 'Không xác định'
                    END
            ) AS trangThaiText,
            -- Thời gian trạng thái gần nhất
            (
                SELECT MAX(lst.thoi_gian)
                FROM lich_su_trang_thai_hoa_don lst
                WHERE lst.hoa_don_id = hd.id
            ) AS thoiGianCapNhatCuoi,
            -- Danh sách IMEI của hóa đơn chi tiết này
            (
                SELECT JSON_ARRAYAGG(
                    JSON_OBJECT(
                        'id', imei.id,
                        'code', imei.code,
                        'imeiCode', imei.code,  -- alias để dễ dùng
                        'status', imei.trang_thai_imei,
                        'statusText',
                            CASE imei.trang_thai_imei
                                WHEN 0 THEN 'Có sẵn'
                                WHEN 1 THEN 'Đã bán'
                                WHEN 2 THEN 'Đang giữ'
                                WHEN 3 THEN 'Lỗi'
                                WHEN 4 THEN 'Đang bảo hành'
                                ELSE 'Không xác định'
                            END
                    )
                )
                FROM imei imei
                WHERE imei.id_invoice_detail = hdct.id
            ) AS danhSachImei,
            -- Số lượng IMEI của hóa đơn chi tiết này
            (
                SELECT COUNT(imei.id)
                FROM imei imei
                WHERE imei.id_invoice_detail = hdct.id
            ) AS soLuongImei
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
        ORDER BY hd.created_date DESC, hdct.created_date
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM invoice_detail hdct
        LEFT JOIN invoice hd ON hdct.id_invoice = hd.id
        WHERE hd.code = :maHoaDon
        """,
            nativeQuery = true)
    Page<ADHoaDonChiTietResponseDetail> getAllHoaDonChiTietResponse(@Param("maHoaDon") String maHoaDon, Pageable pageable);
    @Query(value = """
    SELECT hdct.id 
    FROM Invoice hd 
    LEFT JOIN InvoiceDetail hdct ON hd.id = hdct.invoice.id 
    WHERE hd.id = :id
    """)
    List<String> getAllHoaDonChiTiet(String maHoaDon);

    List<InvoiceDetail> findByInvoiceId(String id);
}