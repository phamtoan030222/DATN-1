package com.sd20201.datn.core.admin.products.product.repository;

import com.sd20201.datn.core.admin.products.product.model.request.ADProductRequest;
import com.sd20201.datn.core.admin.products.product.model.response.ADProductDetailResponse;
import com.sd20201.datn.core.admin.products.product.model.response.ADProductResponse;
import com.sd20201.datn.entity.Product;
import com.sd20201.datn.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ADProductRepository extends ProductRepository {

    @Query(value = """
    SELECT
        p.id as id
        , p.code as code
        , p.name as name
        , p.screen.name as screen
        , p.battery.name as battery
        , p.brand.name as brand
        , p.operatingSystem.name as operatingSystem
        , p.createdDate as createdDate
    FROM Product p
    where
        (
            :#{#request.q} is null or p.name like concat('%',:#{#request.q},'%')
            OR :#{#request.q} is null or p.code like concat('%',:#{#request.q},'%')
        ) AND (:#{#request.idBrand} is NULL OR p.brand.id like concat('%',:#{#request.idBrand},'%'))
          AND (:#{#request.idBattery} is NULL OR p.battery.id like concat('%',:#{#request.idBattery},'%'))
          AND (:#{#request.idScreen} is NULL OR p.screen.id like concat('%',:#{#request.idScreen},'%'))
          AND (:#{#request.idOperatingSystem} is NULL OR p.operatingSystem.id like concat('%',:#{#request.idOperatingSystem},'%'))
    """, countQuery = """
    SELECT
        COUNT(1)
    FROM Product p
    where
        (
            :#{#request.q} is null or p.name like concat('%',:#{#request.q},'%')
            OR :#{#request.q} is null or p.code like concat('%',:#{#request.q},'%')
        ) AND (:#{#request.idBrand} is NULL OR p.brand.id like concat('%',:#{#request.idBrand},'%'))
          AND (:#{#request.idBattery} is NULL OR p.battery.id like concat('%',:#{#request.idBattery},'%'))
          AND (:#{#request.idScreen} is NULL OR p.screen.id = :#{#request.idScreen})
          AND (:#{#request.idOperatingSystem} is NULL OR p.operatingSystem.id like concat('%',:#{#request.idOperatingSystem},'%'))
    """)
    Page<ADProductResponse> getProducts(Pageable pageable, ADProductRequest request);

    @Query(value = """
        SELECT
        p.id as id
        , p.code as code
        , p.name as name
        , p.screen.id as idScreen
        , p.battery.id as idBattery
        , p.brand.id as idBrand
        , p.operatingSystem.id as idOperatingSystem
    FROM Product p
    WHERE id = :id
    """)
    Optional<ADProductDetailResponse> getProductById(@Param("id") String id);

    Optional<Product> findByCode(String code);
}
