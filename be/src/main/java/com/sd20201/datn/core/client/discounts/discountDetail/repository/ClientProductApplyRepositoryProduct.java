package com.sd20201.datn.core.client.discounts.discountDetail.repository;

import com.sd20201.datn.core.client.discounts.discountDetail.model.respone.ClientDiscountDetailRespone;
import com.sd20201.datn.repository.ProductDiscountDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientProductApplyRepositoryProduct extends ProductDiscountDetailRepository {
        @Query(value = """
            SELECT c.id AS id,
                   p.code AS productCode,
                   pd.urlImage AS image,
                   pd.code AS productDetailCode,
                   p.name AS productName,
                   d.name AS discountName,
                   d.code AS discountCode,
                   d.percentage AS percentageDiscount,
                   d.startDate AS startTime,
                   d.endDate AS endTime,
                   pd.price AS price,
                   c.description AS description,
                   co.name AS colorName,
                                  r.name AS ramName,
                                  h.name AS hardDriveName,
                                  m.name AS materialName,
                                  g.name AS gpuName,
                                  cpu.name AS cpuName
            FROM ProductDetailDiscount c
                     JOIN c.productDetail pd
                     JOIN pd.product p
                     JOIN c.discount d
                     LEFT JOIN pd.color co
                                      LEFT JOIN pd.ram r
                                      LEFT JOIN pd.hardDrive h
                                      LEFT JOIN pd.material m
                                      LEFT JOIN pd.gpu g
                                      LEFT JOIN pd.cpu cpu
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
        Page<ClientDiscountDetailRespone> getAllAppliedProductsByDiscount(Pageable pageable,
                                                                          @Param("discountId") String discountId);
    }




