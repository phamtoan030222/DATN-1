package com.sd20201.datn.core.admin.banhang.repository;

import com.sd20201.datn.core.admin.banhang.model.request.ADThemSanPhamRequest;
import com.sd20201.datn.core.admin.banhang.model.request.ListKhachHangRequest;
import com.sd20201.datn.core.admin.banhang.model.response.ADChonKhachHangResponse;
import com.sd20201.datn.core.admin.banhang.model.response.ADGioHangResponse;
import com.sd20201.datn.core.admin.banhang.model.response.ADPhuongThucThanhToanRespones;
import com.sd20201.datn.repository.InvoiceDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ADTaoHoaDonChiTietRepository extends InvoiceDetailRepository {

    @Query(value = """
SELECT
    ROW_NUMBER() OVER (ORDER BY hdct.created_date DESC) AS stt,

    hdct.id                                             AS idHDCT,
    spct.code                                           AS ma,
    spct.id                                             AS id,
    CONCAT(sp.name, ' - ', cl.name)                    AS ten,

    (
        SELECT COUNT(*)
        FROM imei ii
        WHERE ii.id_invoice_detail = hdct.id
    )                                                   AS soLuong,

    -- giá gốc chưa giảm lúc thêm vào giỏ
    hdct.gia_goc                                        AS giaGoc,

    -- giá bán thực tế lúc thêm vào giỏ (không tính lại theo discount hiện tại)
    hdct.price                                          AS giaBan,

    -- giá hiện tại của sản phẩm (để so sánh cảnh báo)
    spct.price                                          AS giaHienTai,

    -- cờ giá thay đổi: so với giá gốc lúc thêm
    CASE
        WHEN hdct.gia_goc <> spct.price THEN 1
        ELSE 0
    END                                                 AS giaDaThayDoi,

    spct.url_image                                      AS anh,

    c.name                                              AS cpu,
    r.name                                              AS ram,
    hd2.name                                            AS hardDrive,
    g.name                                              AS gpu,
    cl.name                                             AS color,

    -- gom tất cả IMEI của dòng chi tiết thành 1 chuỗi
    GROUP_CONCAT(i.code ORDER BY i.code SEPARATOR ', ') AS imel,

    -- % giảm giá hiện tại (chỉ để hiển thị tham khảo)
    discount_info.max_percentage                        AS percentage,

    CASE
        WHEN hd.trang_thai_hoa_don IS NOT NULL
        THEN CAST(hd.trang_thai_hoa_don AS CHAR)
        ELSE 'ACTIVE'
    END                                                 AS status

FROM invoice_detail hdct

JOIN invoice hd
    ON hdct.id_invoice = hd.id

JOIN product_detail spct
    ON hdct.id_product_detail = spct.id

JOIN product sp
    ON spct.id_product = sp.id

LEFT JOIN (
    SELECT
        pdd.id_product_detail,
        MAX(d.percentage) AS max_percentage
    FROM product_detail_discount pdd
    JOIN discount d
        ON pdd.id_discount = d.id
    WHERE pdd.status = 0
      AND d.status = 0
      AND d.start_date <= :currentTime
      AND d.end_date >= :currentTime
    GROUP BY pdd.id_product_detail
) discount_info
    ON spct.id = discount_info.id_product_detail

LEFT JOIN cpu c
    ON spct.id_cpu = c.id

LEFT JOIN ram r
    ON spct.id_ram = r.id

LEFT JOIN hard_drive hd2
    ON spct.id_hard_drive = hd2.id

LEFT JOIN gpu g
    ON spct.id_gpu = g.id

LEFT JOIN color cl
    ON spct.id_color = cl.id

LEFT JOIN imei i
    ON i.id_invoice_detail = hdct.id

WHERE hd.id = :rep

GROUP BY
    hdct.id,
    hdct.created_date,
    hdct.gia_goc,
    hdct.price,
    spct.id,
    spct.code,
    spct.price,
    spct.url_image,
    sp.name,
    c.name,
    r.name,
    hd2.name,
    g.name,
    cl.name,
    hd.trang_thai_hoa_don,
    discount_info.max_percentage

ORDER BY hdct.created_date DESC
""", nativeQuery = true)
    List<ADGioHangResponse> getAllGioHang(
            @Param("rep") String rep,
            @Param("currentTime") Long currentTime
    );
    @Query( value = """
        SELECT kh.id AS id, kh.name AS ten, kh.phone AS sdt , kh.email AS email
        FROM Customer kh
        WHERE kh.status = 0
        AND ((:#{#req.q} IS NULL OR :#{#req.q} = '' OR kh.name LIKE CONCAT('%', :#{#req.q}, '%') OR kh.phone LIKE CONCAT('%', :#{#req.q}, '%'))
         OR (:#{#req.q} IS NULL OR :#{#req.q} = '' OR kh.code LIKE CONCAT('%', :#{#req.q}, '%') OR kh.phone LIKE CONCAT('%', :#{#req.q}, '%'))
         OR (:#{#req.q} IS NULL OR :#{#req.q} = '' OR kh.phone LIKE CONCAT('%', :#{#req.q}, '%') OR kh.phone LIKE CONCAT('%', :#{#req.q}, '%'))
        )  
    """,
            countQuery = """
        SELECT COUNT(*) 
        FROM Customer kh 
        WHERE kh.status = 0 
        AND ((:#{#req.q} IS NULL OR :#{#req.q} = '' OR kh.name LIKE CONCAT('%', :#{#req.q}, '%') OR kh.phone LIKE CONCAT('%', :#{#req.q}, '%'))
         OR (:#{#req.q} IS NULL OR :#{#req.q} = '' OR kh.code LIKE CONCAT('%', :#{#req.q}, '%') OR kh.phone LIKE CONCAT('%', :#{#req.q}, '%'))
         OR (:#{#req.q} IS NULL OR :#{#req.q} = '' OR kh.phone LIKE CONCAT('%', :#{#req.q}, '%') OR kh.phone LIKE CONCAT('%', :#{#req.q}, '%'))
                )
    """)
    Page<ADChonKhachHangResponse> getAllList(@Param("req") ListKhachHangRequest request, Pageable pageable);

    @Query("""
    SELECT 
        kh.id AS id,
        kh.name AS ten,
        kh.phone AS sdt,
        kh.email AS email,
        adr.addressDetail AS diaChi,
        adr.provinceCity AS tinh,
        adr.district AS huyen,
        adr.wardCommune AS xa
    FROM Invoice hd
    LEFT JOIN hd.customer kh
    LEFT JOIN kh.addresses adr
        ON adr.isDefault = true
    WHERE hd.id = :rep
""")
    ADChonKhachHangResponse getKhachHang(@Param("rep") String rep);

    @Query(value= """
    select SUM(hdct.price * hdct.quantity) as tongTien, hd.nameReceiver as phuongThucThanhToan  from Invoice hd 
    left Join InvoiceDetail hdct on hd.id = hdct.invoice.id
    where hd.id = :#{#rep}
""")
    List<ADPhuongThucThanhToanRespones> getPhuongThucThanhToan(@Param("rep") String req);


    @Query(value = """
        SELECT idct.id FROM InvoiceDetail idct
        LEFT JOIN idct.productDetail pd
        WHERE idct.invoice.id = :#{#rep.idHD} 
        AND pd.id = :#{#rep.idSP}
        ORDER BY idct.createdDate DESC
    """)
    List<String> checkGioHang(@Param("rep") ADThemSanPhamRequest req);

    @Query(value = """
    SELECT hdct.id 
    FROM Invoice hd 
    LEFT JOIN InvoiceDetail hdct ON hd.id = hdct.invoice.id 
    WHERE hd.id = :id
    """)
    List<String> getHoaDonChiTiet(@Param("id") String id);

    @Query(value = """
    SELECT lstt.id 
    FROM InvoiceDetail hd 
    LEFT JOIN ProductDetail lstt ON lstt.id = hd.productDetail.id 
    WHERE hd.id = :id
    """)
    String getSanPhamChiTiet(@Param("id") String id);

}
