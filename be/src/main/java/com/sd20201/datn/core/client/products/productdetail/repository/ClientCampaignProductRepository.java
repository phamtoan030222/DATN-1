package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.productdetail.model.response.ClientDiscountProductProjection;
import com.sd20201.datn.repository.ProductDiscountDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientCampaignProductRepository extends ProductDiscountDetailRepository {
    @Query("SELECT d.name AS discountName, d.startDate AS startDate, d.endDate AS endDate, d.percentage AS percentage, " +
            "p.name AS productName, pd.id AS productDetailId, pd.urlImage as urlImage, " +
            "cpu.name AS cpu, gpu.name AS gpu, ram.name AS ram, hd.name AS hardDrive, " +
            "pdd.originalPrice AS originalPrice, pdd.salePrice AS salePrice " +
            "FROM ProductDetailDiscount pdd " +
            "JOIN pdd.discount d " +
            "JOIN pdd.productDetail pd " +
            "JOIN pd.product p " +
            "LEFT JOIN pd.hardDrive hd " +
            "WHERE d.startDate <= :currentTime AND d.endDate >= :currentTime " +
            "AND d.percentage = ( " +
            "    SELECT MAX(d2.percentage) " +
            "    FROM Discount d2 " +
            "    WHERE d2.startDate <= :currentTime AND d2.endDate >= :currentTime " +
            ") " +
            "ORDER BY pdd.salePrice ASC") // <-- Đã đổi sắp xếp thành giá tăng dần vì % lúc này là như nhau
    Page<ClientDiscountProductProjection> findOngoingDiscountProducts(Pageable pageable, @Param("currentTime") Long currentTime);
    // 2. Lấy ID đợt giảm giá SẮP diễn ra gần nhất
    @Query("SELECT d.id FROM Discount d " +
            "WHERE d.startDate > :currentTime " +
            "ORDER BY d.startDate ASC")
    Page<String> findNearestUpcomingDiscountId(@Param("currentTime") Long currentTime, Pageable pageable);

    // 3. Lấy sản phẩm của đợt giảm giá theo ID
    @Query("SELECT d.name AS discountName, d.startDate AS startDate, d.endDate AS endDate, d.percentage AS percentage, " +
            "p.name AS productName, pd.id AS productDetailId, pd.urlImage as urlImage," +
            "cpu.name AS cpu, gpu.name AS gpu, ram.name AS ram, hd.name AS hardDrive," +
            "pdd.originalPrice AS originalPrice, pdd.salePrice AS salePrice " +
            "FROM ProductDetailDiscount pdd " +
            "JOIN pdd.discount d " +
            "JOIN pdd.productDetail pd " +
            "JOIN pd.product p " +
            "LEFT JOIN pd.hardDrive hd " +
            "WHERE d.id = :discountId")
    Page<ClientDiscountProductProjection> findProductsByDiscountId(Pageable pageable, @Param("discountId") String discountId);

}
