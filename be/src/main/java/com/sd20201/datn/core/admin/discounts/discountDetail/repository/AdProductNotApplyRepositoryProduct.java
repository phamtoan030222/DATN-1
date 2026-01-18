package com.sd20201.datn.core.admin.discounts.discountDetail.repository;

import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdProductDetailResponse;
import com.sd20201.datn.repository.ProductDiscountDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdProductNotApplyRepositoryProduct extends ProductDiscountDetailRepository {
    @Query(value = """
        SELECT pd.id AS id,
               p.code AS productCode,
               pd.code AS productDetailCode,
               pd.urlImage AS image,
               p.name AS productName,
               c.name AS colorName,
               r.name AS ramName,
               h.name AS hardDriveName,
               m.name AS materialName,
               g.name AS gpuName,
               cpu.name AS cpuName,
               pd.price AS price,
               pd.description AS description
        FROM ProductDetail pd
                 JOIN pd.product p
                 JOIN pd.color c
                 JOIN pd.ram r
                 JOIN pd.hardDrive h
                 JOIN pd.material m
                 JOIN pd.gpu g
                 JOIN pd.cpu cpu
        WHERE pd.status = 0
          AND NOT EXISTS (
              SELECT 1
              FROM ProductDetailDiscount d
              WHERE d.productDetail.id = pd.id
                AND d.discount.id = :discountId
                AND d.status = 0
          )
        ORDER BY pd.createdDate DESC
        """,
            countQuery = """
        SELECT COUNT(pd.id)
        FROM ProductDetail pd
                 JOIN pd.product p
        WHERE pd.status = 0
          AND NOT EXISTS (
              SELECT 1
              FROM ProductDetailDiscount d
              WHERE d.productDetail.id = pd.id
                AND d.discount.id = :discountId
                AND d.status = 0
          )
        """)
    Page<AdProductDetailResponse> getAllNotAppliedProductsByDiscount(Pageable pageable,
                                                                     @Param("discountId") String discountId);


}
