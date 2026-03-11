package com.sd20201.datn.core.admin.discounts.discount.repository;

import com.sd20201.datn.core.admin.discounts.discount.model.response.AdDiscountResponse;
import com.sd20201.datn.entity.Discount;
import com.sd20201.datn.repository.DiscountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdDiscountRepossitory  extends DiscountRepository {

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
                  c.status AS status ,
               (SELECT COUNT(DISTINCT pdd.productDetail.product.id)\s
                                     FROM ProductDetailDiscount pdd\s
                                     WHERE pdd.discount.id = c.id AND pdd.status=0) AS productCount
           FROM Discount c 
           WHERE (:discountName IS NULL OR c.name LIKE CONCAT('%', :discountName, '%'))
                 AND (:status IS NULL OR c.status = :status)
           ORDER BY c.startDate DESC
    """,
            countQuery = """
           SELECT COUNT(c.id)
           FROM Discount c 
           WHERE (:discountName IS NULL OR c.name LIKE CONCAT('%', :discountName, '%'))
                 AND (:status IS NULL OR c.status = :status)
    """
    )
    Page<AdDiscountResponse> getAllDiscount(Pageable pageable,
                                            @Param("discountName") String discountName,
                                            @Param("status") Integer status);

    List<Discount> findAlDiscountByCode(String code);
    List<Discount> findAllDiscountByName(String name);

    @Query("SELECT v FROM Discount v WHERE v.name = :discountName AND v.id <> :id")
    List<Discount> findByNameAndNotId(@Param("discountName") String discountName,
                                      @Param("id") String id);

    @Query("SELECT v FROM Discount v WHERE v.code = :discountCode AND v.id <> :id")
    List<Discount> findByCodeAndNotId(@Param("discountCode") String discountCode,
                                     @Param("id") String id);


//    @Query("SELECT d FROM Discount d WHERE d.status = 'ACTIVE' AND " +
//            "NOT (d.endDate <= :startDate OR d.startDate >= :endDate)")
//    List<Discount> findOverlappingDiscounts(@Param("startDate") Long startDate,
//                                            @Param("endDate") Long endDate);

}
