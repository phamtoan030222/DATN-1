package com.sd20201.datn.core.admin.discounts.discountDetail.repository;

import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdProductDetailResponse;
import com.sd20201.datn.repository.ProductDetailRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdProductDetailRepository extends ProductDetailRepository {
    @Query("""
            SELECT pd.id AS id,
                   p.code AS productCode,
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
            WHERE p.id = :productId
            """)
    List<AdProductDetailResponse> findProductDetailsByProductId(@Param("productId") String productId);
}
