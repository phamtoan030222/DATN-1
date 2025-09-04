package com.sd20201.datn.core.admin.discounts.discountDetail.repository;

import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdDiscountDetailRespone;
import com.sd20201.datn.repository.ProductDiscountDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdProductApplyRepositoryProduct extends ProductDiscountDetailRepository {
        @Query(value = """
            SELECT c.id AS id,
                   p.code AS productCode,
                   p.name AS productName,
                   d.name AS discountName,
                   d.code AS discountCode,
                   d.percentage AS percentageDiscount,
                   d.startDate AS startTime,
                   d.endDate AS endTime,
                   pd.price AS price,
                   c.description AS description
            FROM ProductDetailDiscount c
                     JOIN c.productDetail pd
                     JOIN pd.product p
                     JOIN c.discount d
            WHERE d.id = :discountId
            AND c.status =0 
            AND pd.status =0 
            ORDER BY c.createdDate DESC
            """,
                countQuery = """
            SELECT COUNT(c.id)
            FROM ProductDetailDiscount c
                     JOIN c.productDetail pd
                     JOIN pd.product p
                     JOIN c.discount d
            WHERE d.id = :discountId
            AND c.status = 0
            """)
        Page<AdDiscountDetailRespone> getAllAppliedProductsByDiscount(Pageable pageable,
                                                                      @Param("discountId") String discountId);
    }




