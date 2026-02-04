package com.sd20201.datn.core.admin.products.product.repository;

import com.sd20201.datn.core.admin.products.product.model.request.ADProductRequest;
import com.sd20201.datn.core.admin.products.product.model.response.ADPRPropertyComboboxResponse;
import com.sd20201.datn.core.admin.products.product.model.response.ADProductDetailResponse;
import com.sd20201.datn.core.admin.products.product.model.response.ADProductResponse;
import com.sd20201.datn.entity.Product;
import com.sd20201.datn.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ADProductRepository extends ProductRepository {

    @Query(value = """
    SELECT
        p.id as id
        , p.code as code
        , p.name as name
        , p.status as status
        , p.screen.name as screen
        , p.battery.name as battery
        , p.brand.name as brand
        , p.operatingSystem.name as operatingSystem
        , MIN(pd.price) as minPrice
        , MAX(pd.price) as maxPrice
        , COUNT(pd.id) as quantity
        , ip.url as urlImage
        , MAX(d.percentage) as percentage
    FROM Product p
        LEFT JOIN ProductDetail pd on p.id = pd.product.id
        LEFT JOIN ImageProduct ip on p.id = ip.product.id
        LEFT join ProductDetailDiscount pdd on pd.id = pdd.productDetail.id
        LEFT JOIN Discount d on pdd.discount.id = d.id
    where
        (
            :#{#request.q} is null or p.name like concat('%',:#{#request.q},'%')
            OR :#{#request.q} is null or p.code like concat('%',:#{#request.q},'%')
        ) AND (:#{#request.idBrand} is NULL OR p.brand.id like concat('%',:#{#request.idBrand},'%'))
          AND (:#{#request.idBattery} is NULL OR p.battery.id like concat('%',:#{#request.idBattery},'%'))
          AND (:#{#request.idScreen} is NULL OR p.screen.id like concat('%',:#{#request.idScreen},'%'))
          AND (:#{#request.idOperatingSystem} is NULL OR p.operatingSystem.id like concat('%',:#{#request.idOperatingSystem},'%'))
          AND (ip.index = 1 or ip.id is null)
          AND ((d.startDate <= :date and :date <= d.endDate AND d.status=0) OR d.id IS NULL)
    GROUP BY     p.id,
                 p.code,
                 p.name,
                 p.status,
                 p.screen,
                 p.battery,
                 p.brand,
                 p.operatingSystem,
                 ip.url
    HAVING (:#{#request.minPrice} <= MIN(pd.price) AND MAX(pd.price) <= :#{#request.maxPrice})
    order by p.createdDate desc
    """, countQuery = """
    SELECT
        COUNT(1)
    FROM Product p
        LEFT JOIN ProductDetail pd on p.id = pd.product.id
        LEFT JOIN ImageProduct ip on p.id = ip.product.id
        LEFT join ProductDetailDiscount pdd on pd.id = pdd.productDetail.id
        LEFT JOIN Discount d on pdd.discount.id = d.id
    where
        (
            :#{#request.q} is null or p.name like concat('%',:#{#request.q},'%')
            OR :#{#request.q} is null or p.code like concat('%',:#{#request.q},'%')
        ) AND (:#{#request.idBrand} is NULL OR p.brand.id like concat('%',:#{#request.idBrand},'%'))
          AND (:#{#request.idBattery} is NULL OR p.battery.id like concat('%',:#{#request.idBattery},'%'))
          AND (:#{#request.idScreen} is NULL OR p.screen.id like concat('%',:#{#request.idScreen},'%'))
          AND (:#{#request.idOperatingSystem} is NULL OR p.operatingSystem.id like concat('%',:#{#request.idOperatingSystem},'%'))
          AND (ip.index = 1 or ip.id is null)
          AND ((d.startDate <= :date and :date <= d.endDate AND d.status=0) OR d.id IS NULL)
    GROUP BY     p.id,
                 p.code,
                 p.name,
                 p.status,
                 p.screen,
                 p.battery,
                 p.brand,
                 p.operatingSystem,
                 ip.url
    HAVING (:#{#request.minPrice} <= MIN(pd.price) AND MAX(pd.price) <= :#{#request.maxPrice})
    """)
    Page<ADProductResponse> getProducts(Pageable pageable, ADProductRequest request, Long date);

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

    @Query(value = """
    SELECT p.id as value, p.name as label FROM Product p
    ORDER BY p.createdDate DESC
    """)
    List<ADPRPropertyComboboxResponse> getPropertyCombobox();
}
