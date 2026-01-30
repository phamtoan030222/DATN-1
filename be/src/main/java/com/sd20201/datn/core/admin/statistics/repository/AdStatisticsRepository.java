package com.sd20201.datn.core.admin.statistics.repository;

import com.sd20201.datn.core.admin.statistics.model.response.AdChartResponse;
import com.sd20201.datn.core.admin.statistics.model.response.AdProductResponse;
import com.sd20201.datn.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AdStatisticsRepository extends JpaRepository<Invoice, String> {

    @Query(value = """
        SELECT 
            COALESCE(SUM(CASE WHEN i.trang_thai_hoa_don = 4 THEN i.total_amount_after_decrease ELSE 0 END), 0) as revenue,
            COALESCE(SUM(CASE WHEN i.trang_thai_hoa_don != 5 THEN i.total_amount_after_decrease ELSE 0 END), 0) as expectedRevenue,
            COALESCE((
                SELECT CAST(SUM(id_det.quantity) AS SIGNED)
                FROM invoice_detail id_det 
                JOIN invoice inv_sub ON id_det.id_invoice = inv_sub.id 
                WHERE inv_sub.status = 0 
                AND inv_sub.trang_thai_hoa_don = 4
                AND inv_sub.created_date BETWEEN :start AND :end
            ), 0) as soldProducts,
            
            COUNT(i.id) as totalOrders,
                        SUM(CASE WHEN i.trang_thai_hoa_don = 4 THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN i.trang_thai_hoa_don = 5 THEN 1 ELSE 0 END) as cancelled,
            SUM(CASE WHEN i.trang_thai_hoa_don IN (0,1,2,3,6) THEN 1 ELSE 0 END) as processing
            
        FROM invoice i 
        WHERE i.status = 0 
        AND i.created_date BETWEEN :start AND :end
        """, nativeQuery = true)
    Map<String, Object> getStatsByPeriod(@Param("start") Long start, @Param("end") Long end);


    @Query(value = """
        SELECT p.name, 
               CAST(SUM(id.quantity) AS SIGNED) as count, 
               MAX(pd.price) as price, 
               pd.url_image as image 
        FROM invoice_detail id
        JOIN product_detail pd ON id.id_product_detail = pd.id
        JOIN product p ON pd.id_product = p.id
        JOIN invoice i ON id.id_invoice = i.id
        WHERE i.status = 0 
        AND i.trang_thai_hoa_don = 4
        GROUP BY p.name, pd.id 
        ORDER BY count DESC 
        LIMIT 5
        """, nativeQuery = true)
    List<Object[]> getTopSellingProductsWithDetail();

    @Query(value = """
        SELECT CAST(created_date AS CHAR) as name, 
               CAST(total_amount_after_decrease AS SIGNED) as value 
        FROM invoice 
        WHERE status = 0 
        AND trang_thai_hoa_don = 4 
        AND created_date BETWEEN :start AND :end
        """, nativeQuery = true)
    List<AdChartResponse> getRawRevenueData(@Param("start") Long start, @Param("end") Long end);


    @Query(value = """
        SELECT 
            (CASE WHEN trang_thai_hoa_don = 0 THEN 'Chờ xác nhận' 
                  WHEN trang_thai_hoa_don = 1 THEN 'Đã xác nhận' 
                  WHEN trang_thai_hoa_don = 2 THEN 'Chờ giao' 
                  WHEN trang_thai_hoa_don = 3 THEN 'Đang giao'
                  WHEN trang_thai_hoa_don = 4 THEN 'Hoàn thành'
                  WHEN trang_thai_hoa_don = 5 THEN 'Đã hủy'
                  WHEN trang_thai_hoa_don = 6 THEN 'Lưu tạm'
                  ELSE 'Khác' END) as name, 
            COUNT(*) as value 
        FROM invoice 
        WHERE status = 0 
        AND created_date BETWEEN :start AND :end 
        GROUP BY trang_thai_hoa_don
        """, nativeQuery = true)
    List<AdChartResponse> getFullOrderStatusStats(@Param("start") Long start, @Param("end") Long end);


    @Query(value = """
        SELECT p.name AS name, CAST(SUM(id.quantity) AS signed) AS value 
        FROM invoice_detail id
        JOIN product_detail pd ON id.id_product_detail = pd.id
        JOIN product p ON pd.id_product = p.id
        JOIN invoice i ON id.id_invoice = i.id
        WHERE i.status = 0 
        AND i.trang_thai_hoa_don = 4
        AND i.created_date BETWEEN :start AND :end
        GROUP BY p.name 
        ORDER BY value DESC
        """, nativeQuery = true)
    List<AdChartResponse> getTopSellingProductsChart(@Param("start") Long start, @Param("end") Long end, Pageable pageable);


    @Query(value = """
        SELECT p.id AS id, p.name AS name, b.name AS brandName, 
               CAST(COUNT(pd.id) AS signed) AS quantity, MAX(p.created_date) AS createdDate
        FROM product p
        LEFT JOIN product_detail pd ON p.id = pd.id_product AND pd.status = 0 
        LEFT JOIN brand b ON p.id_brand = b.id
        WHERE p.status = 0 
        GROUP BY p.id, p.name, b.name
        HAVING COUNT(pd.id) <= :limit 
        ORDER BY quantity ASC
        """, nativeQuery = true)
    Page<AdProductResponse> getLowStockProducts(@Param("limit") Integer limit, Pageable pageable);
}