package com.sd20201.datn.core.admin.statistics.repository;

import com.sd20201.datn.core.admin.statistics.model.response.AdChartResponse;
import com.sd20201.datn.core.admin.statistics.model.response.AdProductResponse;
import com.sd20201.datn.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdProductDiscountRepository1 extends JpaRepository<Product, String> {
    // 1. Chart Top Sản phẩm bán chạy (Đếm từ invoice_detail)
    @Query(value = """
        SELECT p.name AS name, 
               CAST(SUM(id.quantity) AS signed) AS value
        FROM invoice_detail id
        JOIN product_detail pd ON id.id_product_detail = pd.id
        JOIN product p ON pd.id_product = p.id
        JOIN invoice i ON id.id_invoice = i.id
        WHERE i.status = 0
        AND i.created_date BETWEEN :start AND :end
        GROUP BY p.name
        ORDER BY value DESC
    """, nativeQuery = true)
    List<AdChartResponse> getTopSellingProductsChart(@Param("start") Long start,
                                                     @Param("end") Long end,
                                                     Pageable pageable);

    // 2. Danh sách sản phẩm sắp hết hàng (Logic đếm dòng product_detail có status=0)
    @Query(value = """
        SELECT p.id AS id, 
               p.name AS name, 
               b.name AS brandName, 
               CAST(COUNT(pd.id) AS signed) AS quantity, 
               MAX(p.created_date) AS createdDate
        FROM product p
        LEFT JOIN product_detail pd ON p.id = pd.id_product AND pd.status = 0 
        LEFT JOIN brand b ON p.id_brand = b.id
        WHERE p.status = 0 
        GROUP BY p.id, p.name, b.name
        HAVING COUNT(pd.id) <= :limit
        ORDER BY quantity ASC
    """,
            countQuery = """
        SELECT COUNT(*) FROM (
            SELECT p.id 
            FROM product p
            LEFT JOIN product_detail pd ON p.id = pd.id_product AND pd.status = 0 
            GROUP BY p.id
            HAVING COUNT(pd.id) <= :limit
        ) as temp
    """, nativeQuery = true)
    Page<AdProductResponse> getLowStockProducts(@Param("limit") Integer limit, Pageable pageable);

    // 3. Count
    @Query(value = "SELECT COUNT(*) FROM product WHERE status = 0 ", nativeQuery = true)
    Integer countAllProducts();

    // 4. Count sắp hết
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT p.id 
            FROM product p
            LEFT JOIN product_detail pd ON p.id = pd.id_product AND pd.status = 0 
            WHERE p.status = 0 
            GROUP BY p.id
            HAVING COUNT(pd.id) <= :limit
        ) as temp
    """, nativeQuery = true)
    Integer countLowStockProducts(@Param("limit") Integer limit);

    // 5. Top 3 Sản phẩm bán chạy nhất (Cho Card Overview)
    @Query(value = """
        SELECT p.name AS name, 
               CAST(SUM(id.quantity) AS signed) AS value
        FROM invoice_detail id
        JOIN product_detail pd ON id.id_product_detail = pd.id
        JOIN product p ON pd.id_product = p.id
        JOIN invoice i ON id.id_invoice = i.id
        WHERE i.status = 0 
        GROUP BY p.name
        ORDER BY value DESC
        LIMIT 3
    """, nativeQuery = true)
    List<AdChartResponse> getTop3BestSelling();

    // 6. Top 3 Thương hiệu có nhiều sản phẩm nhất
    @Query(value = """
        SELECT b.name AS name, 
               CAST(COUNT(p.id) AS signed) AS value
        FROM brand b
        LEFT JOIN product p ON b.id = p.id_brand
        WHERE p.status = 0
        GROUP BY b.id, b.name
        ORDER BY value DESC
        LIMIT 3
    """, nativeQuery = true)
    List<AdChartResponse> getTop3Brands();
}
