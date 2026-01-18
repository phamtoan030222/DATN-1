package com.sd20201.datn.core.admin.statistics.repository;

import com.sd20201.datn.core.admin.statistics.model.response.AdChartResponse;
import com.sd20201.datn.repository.InvoiceRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
@Repository
public interface AdInvoiceRepository extends InvoiceRepository {
    // 1. Lấy dữ liệu thô Chart Doanh Thu (Native Query)
    @Query(value = """
        SELECT CAST(created_date AS CHAR) AS name, 
               CAST(total_amount AS SIGNED) AS value 
        FROM invoice 
        WHERE status = 0 
        AND created_date BETWEEN :start AND :end
    """, nativeQuery = true)
    List<AdChartResponse> getRawRevenueData(@Param("start") Long start, @Param("end") Long end);

    // 2. Chart trạng thái đơn hàng
    @Query(value = """
        SELECT 
           (CASE WHEN status = 0 THEN 'Đã thanh toán' 
                 WHEN status = 1 THEN 'Chờ xác nhận' 
                 WHEN status = 2 THEN 'Đơn huỷ' 
                 WHEN status = 3 THEN 'Đang xử lý' ELSE 'Khác' END) AS name, 
           COUNT(*) AS value 
        FROM invoice 
        WHERE created_date BETWEEN :start AND :end 
        GROUP BY status
    """, nativeQuery = true)
    List<AdChartResponse> getOrderStatusStats(@Param("start") Long start, @Param("end") Long end);
    // 3. Các hàm tính tổng (Native Query để tránh lỗi Enum)
    @Query(value = "SELECT COALESCE(CAST(SUM(total_amount) AS SIGNED), 0) FROM invoice WHERE status = 0", nativeQuery = true)
    Long sumTotalAmount();

    // Tổng doanh thu theo khoảng thời gian
    @Query(value = "SELECT COALESCE(CAST(SUM(total_amount) AS SIGNED), 0) FROM invoice WHERE status = 0 AND created_date BETWEEN :start AND :end", nativeQuery = true)
    Long sumTotalAmountBetween(@Param("start") Long start, @Param("end") Long end);

    @Query(value = "SELECT COUNT(*) FROM invoice WHERE status = :status", nativeQuery = true)
    Integer countByStatus(@Param("status") Integer status);

    @Query(value = "SELECT COUNT(*) FROM invoice WHERE status = 0 AND created_date BETWEEN :start AND :end", nativeQuery = true)
    Integer countOrdersBetween(@Param("start") Long start, @Param("end") Long end);

    @Query(value = "SELECT COUNT(*) FROM invoice", nativeQuery = true)
    long countAllInvoices();

    @Query(value = """
        SELECT i.code, i.name_receiver, i.phone_receiver, i.total_amount, i.created_date, i.status 
        FROM invoice i 
        ORDER BY i.created_date DESC 
        LIMIT 100
    """, nativeQuery = true)
    List<Object[]> getRecentOrdersForExport();

}
