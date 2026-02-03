package com.sd20201.datn.core.client.banhang.repository;

import com.sd20201.datn.core.client.banhang.model.request.ClientThemSanPhamRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientListKhachHangRequest;
import com.sd20201.datn.core.client.banhang.model.response.ClientChonKhachHangResponse;
import com.sd20201.datn.core.client.banhang.model.response.ClientCartItemResponse;
import com.sd20201.datn.core.client.banhang.model.response.ClientPhuongThucThanhToanRespones;
import com.sd20201.datn.repository.InvoiceDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientTaoHoaDonChiTietRepository extends InvoiceDetailRepository {

    @Query(value = """
                SELECT
                    ROW_NUMBER() OVER (ORDER BY hdct.created_date DESC) AS stt,
                    hdct.id AS idHDCT,
                    spct.id AS id,
                    sp.name AS ten,
                    hdct.quantity AS soLuong,
                    spct.price AS giaBan,
            
                    -- [SỬA LỖI 1]: Logic lấy giá giảm tốt nhất
                    CAST(
                        spct.price * (100 - COALESCE(MAX(d.percentage), 0)) / 100
                        AS DECIMAL(19,2)
                    ) AS giaSauGiam,
            
                    spct.url_image AS anh,
                    c.name AS cpu,
                    r.name AS ram,
                    hd2.name AS hardDrive,
                    g.name AS gpu,
                    cl.name AS color,
                    GROUP_CONCAT(DISTINCT i.code SEPARATOR ', ') AS imel 
            
                FROM invoice_detail hdct
                JOIN invoice hd ON hdct.id_invoice = hd.id
                JOIN product_detail spct ON hdct.id_product_detail = spct.id
                JOIN product sp ON spct.id_product = sp.id
                LEFT JOIN cpu c ON spct.id_cpu = c.id
                LEFT JOIN ram r ON spct.id_ram = r.id
                LEFT JOIN hard_drive hd2 ON spct.id_hard_drive = hd2.id
                LEFT JOIN gpu g ON spct.id_gpu = g.id
                LEFT JOIN color cl ON spct.id_color = cl.id
                LEFT JOIN imei i ON hdct.id = i.id_invoice_detail
                LEFT JOIN product_detail_discount pdd ON spct.id = pdd.id_product_detail AND pdd.status = 0
                LEFT JOIN discount d ON pdd.id_discount = d.id 
                     AND d.status = 0 
                     AND :now BETWEEN d.start_date AND d.end_date
                WHERE hd.id = :rep
                GROUP BY hdct.id, spct.id, sp.name, hdct.quantity, spct.price, spct.url_image, 
                         c.name, r.name, hd2.name, g.name, cl.name, hdct.created_date
                ORDER BY hdct.created_date DESC
            """, nativeQuery = true)
    List<ClientCartItemResponse> getAllGioHang(@Param("rep") String rep, @Param("now") Long now);

    @Query(value = """
                SELECT kh.id AS id, kh.name AS ten, kh.phone AS sdt
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
    Page<ClientChonKhachHangResponse> getAllList(@Param("req") ClientListKhachHangRequest request, Pageable pageable);

    @Query("""
                SELECT 
                    kh.id AS id,
                    kh.name AS ten,
                    kh.phone AS sdt,
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
    ClientChonKhachHangResponse getKhachHang(@Param("rep") String rep);

    @Query(value = """
                select SUM(hdct.price * hdct.quantity) as tongTien, hd.nameReceiver as phuongThucThanhToan  from Invoice hd 
                left Join InvoiceDetail hdct on hd.id = hdct.invoice.id
                where hd.id = :#{#rep}
            """)
    List<ClientPhuongThucThanhToanRespones> getPhuongThucThanhToan(@Param("rep") String req);


    @Query(value = """
                SELECT idct.id FROM InvoiceDetail idct
                LEFT JOIN idct.productDetail pd
                WHERE idct.invoice.id = :#{#rep.idHD} 
                AND pd.id = :#{#rep.idSP}
                ORDER BY idct.createdDate DESC
            """)
    List<String> checkGioHang(@Param("rep") ClientThemSanPhamRequest req);

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
