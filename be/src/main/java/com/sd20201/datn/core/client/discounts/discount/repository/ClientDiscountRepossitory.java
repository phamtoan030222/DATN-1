package com.sd20201.datn.core.client.discounts.discount.repository;

import com.sd20201.datn.core.client.discounts.discount.model.response.ClientDiscountResponse;
import com.sd20201.datn.entity.Discount;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.repository.DiscountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientDiscountRepossitory extends DiscountRepository {

    @Query(
            value = """
                           SELECT c.id AS id,
                                  c.name AS discountName,
                                  c.code AS discountCode,
                                  c.createdDate AS createdDate,
                                  c.startDate AS startTime,
                                  c.endDate AS endTime,
                                  c.percentage AS percentage,
                                  c.description AS description,
                                  c.status AS discountStatus ,
                               (SELECT COUNT(DISTINCT pdd.productDetail.product.id)\s
                                                     FROM ProductDetailDiscount pdd\s
                                                     WHERE pdd.discount.id = c.id AND pdd.status=0) AS productCount
                           FROM Discount c 
                           WHERE (:discountName IS NULL OR c.name LIKE CONCAT('%', :discountName, '%'))
                                 AND (:discountStatus IS NULL OR c.status = :discountStatus)
                                 AND c.status = 0
                           ORDER BY c.startDate DESC
                    """,
            countQuery = """
                           SELECT COUNT(c.id)
                           FROM Discount c
                           WHERE (:discountName IS NULL OR c.name LIKE CONCAT('%', :discountName, '%'))
                                 AND (:discountStatus IS NULL OR c.status = :discountStatus)
                    """
    )
    Page<ClientDiscountResponse> getAllDiscount(Pageable pageable,
                                                @Param("discountName") String discountName,
                                                @Param("discountStatus") Integer discountStatus);

    List<Discount> findAlDiscountByCode(String code);

    List<Discount> findAllDiscountByName(String name);

    @Query("SELECT v FROM Discount v WHERE v.name = :discountName AND v.id <> :id")
    List<Discount> findByNameAndNotId(@Param("discountName") String discountName,
                                      @Param("id") String id);

    @Query("SELECT v FROM Discount v WHERE v.code = :discountCode AND v.id <> :id")
    List<Discount> findByCodeAndNotId(@Param("discountCode") String discountCode,
                                      @Param("id") String id);

    @Query("SELECT d FROM Discount d " +
            "WHERE d.startDate <= :currentTime " +
            "AND d.endDate >= :currentTime " +
            "AND d.status = :status " +
            "ORDER BY d.endDate ASC")
    List<ClientDiscountResponse> findActiveDiscounts(@Param("currentTime") Long currentTime);

    @Query("""
                SELECT 
                    d.id AS id,
                    d.name AS discountName,
                    d.code AS discountCode,
                    d.createdDate AS createdDate,
                    d.startDate AS startTime,
                    d.endDate AS endTime,
                    d.percentage AS percentage,
                    d.description AS description,
                    COUNT(pdd.id) AS productCount
                FROM Discount d
                LEFT JOIN ProductDetailDiscount pdd ON d.id = pdd.discount.id AND pdd.status = :status
                WHERE d.startDate <= :currentTime 
                  AND d.endDate >= :currentTime 
                  AND d.status = :status
                GROUP BY 
                    d.id, 
                    d.name, 
                    d.code, 
                    d.createdDate, 
                    d.startDate, 
                    d.endDate, 
                    d.percentage, 
                    d.description
                ORDER BY d.endDate ASC
            """)
    List<ClientDiscountResponse> getActiveDiscounts(
            @Param("currentTime") Long currentTime,
            EntityStatus active);
}
