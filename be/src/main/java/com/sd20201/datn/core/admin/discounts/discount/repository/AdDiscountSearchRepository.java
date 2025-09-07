package com.sd20201.datn.core.admin.discounts.discount.repository;

import com.sd20201.datn.core.admin.discounts.discount.model.response.AdDiscountResponse;
import com.sd20201.datn.repository.DiscountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface  AdDiscountSearchRepository extends DiscountRepository {

    @Query(
            value = """
            SELECT d.id AS id,
                   d.name AS discountName,
                   d.code AS discountCode,
                   d.createdDate AS createdDate,
                   d.startDate AS startTime,
                   d.endDate AS endTime,
                   d.percentage AS percentage,
                   d.description AS description
            FROM Discount d
            WHERE (:discountName IS NULL OR d.name LIKE %:discountName%)
              AND (:discountCode IS NULL OR d.code LIKE %:discountCode%)
              AND (:discountStatus IS NULL OR d.status = :discountStatus)
              AND (:percentage IS NULL OR d.percentage = :percentage)
              AND (:description IS NULL OR d.description LIKE %:description%)
              AND (:startDate IS NULL OR d.startDate >= :startDate)
              AND (:endDate IS NULL OR d.endDate <= :endDate)
              AND d.status = 0
            ORDER BY d.createdDate DESC
            """,
            countQuery = """
            SELECT COUNT(d.id)
            FROM Discount d
              WHERE (:discountName IS NULL OR d.name LIKE CONCAT('%', :discountName, '%'))
              AND (:discountCode IS NULL OR d.code LIKE CONCAT('%', :discountCode, '%'))
              AND (:discountStatus IS NULL OR d.status = :discountStatus)
              AND (:percentage IS NULL OR d.percentage = :percentage)
              AND (:description IS NULL OR d.description LIKE CONCAT('%', :description, '%'))
              AND (:startDate IS NULL OR d.startDate >= :startDate)
              AND (:endDate IS NULL OR d.endDate <= :endDate)
              AND d.status = 0
            
            """
    )
    Page<AdDiscountResponse> filterDiscounts(Pageable pageable,
                                             @Param("discountName") String discountName,
                                             @Param("discountCode") String discountCode,
                                             @Param("discountStatus") Integer discountStatus,
                                             @Param("percentage") Integer percentage,
                                             @Param("description") String description,
                                             @Param("startDate") Long startDate,
                                             @Param("endDate") Long endDate);
}
