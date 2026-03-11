package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDExistVariantRequest;
import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDProductDetailRequest;
import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDPriceMinMaxResponse;
import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDProductDetailDetailResponse;
import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDProductDetailResponse;
import com.sd20201.datn.entity.*;
import com.sd20201.datn.repository.ProductDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientPDProductDetailRepository extends ProductDetailRepository {

    // ==================================================================================================
    // 1. Query thường (Không có giảm giá) - Đã tối ưu logic WHERE
    // ==================================================================================================
    @Query(value = """
            SELECT
                    p.id as id, p.code as code, p.name as name, p.description as description
                    , p.hardDrive.name as hardDrive, p.material.name as material
                    , p.color.name as color, p.gpu.name as gpu, p.cpu.name as cpu, p.ram.name as ram
                    , p.price as price, p.status as status
                    , (SELECT COUNT(i.id) FROM IMEI i WHERE i.productDetail.id = p.id AND (i.imeiStatus = 0 OR i.imeiStatus = 1)) as quantity
                    , p.urlImage as urlImage, p.product.name as productName, p.product.screen.name as screenName
                    , p.product.brand.name as brandName, p.product.battery.name as batteryName
                    , p.product.operatingSystem.name as operatingSystemName
            FROM ProductDetail p
            WHERE (:#{#request.q} IS NULL OR p.name LIKE %:#{#request.q}% OR p.code LIKE %:#{#request.q}% OR p.product.name LIKE %:#{#request.q}%)
              AND (:#{#request.idGPU} IS NULL OR p.gpu.id IN :#{#request.idGPU})
              AND (:#{#request.idCPU} IS NULL OR p.cpu.id IN :#{#request.idCPU})
              AND (:#{#request.idColor} IS NULL OR p.color.id IN :#{#request.idColor})
              AND (:#{#request.idMaterial} IS NULL OR p.material.id IN :#{#request.idMaterial})
              AND (:#{#request.idHardDrive} IS NULL OR p.hardDrive.id IN :#{#request.idHardDrive})
              AND (:#{#request.idRAM} IS NULL OR p.ram.id IN :#{#request.idRAM})
              AND (:#{#request.idProduct} IS NULL OR p.product.id IN :#{#request.idProduct})
              AND (:#{#request.idBrand} IS NULL OR p.product.brand.id IN :#{#request.idBrand})
              AND (:#{#request.idScreen} IS NULL OR p.product.screen.id IN :#{#request.idScreen})
              AND (:#{#request.minPrice} IS NULL OR p.price >= :#{#request.minPrice})
              AND (:#{#request.maxPrice} IS NULL OR p.price <= :#{#request.maxPrice})
            GROUP BY p.id, p.code, p.name, p.description, p.price, p.status, p.urlImage, p.createdDate,
                     p.hardDrive.name, p.material.name, p.color.name, p.gpu.name, p.cpu.name, p.ram.name,
                     p.product.name, p.product.screen.name, p.product.brand.name, p.product.battery.name, p.product.operatingSystem.name
            ORDER BY p.createdDate DESC
            """)
    Page<ClientPDProductDetailResponse> getProductDetails(Pageable pageable, @Param("request") ClientPDProductDetailRequest request);

    // ==================================================================================================
    // 2. Query có Discount - Đã sửa lỗi "Râu ông nọ cắm cằm bà kia"
    // ==================================================================================================
    @Query(value = """
        SELECT
             p.id as id, p.code as code, p.name as name, p.description as description
            , p.hardDrive.name as hardDrive, p.material.name as material, p.color.name as color
            , p.gpu.name as gpu, p.cpu.name as cpu, p.ram.name as ram
            , p.price as price, p.status as status
            , (SELECT COUNT(i.id) FROM IMEI i WHERE i.productDetail.id = p.id AND (i.imeiStatus = 0 OR i.imeiStatus = 1)) as quantity
            , p.urlImage as urlImage, p.product.name as productName, p.product.screen.name as screenName
            , p.product.brand.name as brandName, p.product.battery.name as batteryName
            , p.product.operatingSystem.name as operatingSystemName
            , (SELECT MAX(d2.percentage) FROM ProductDetailDiscount pdd2 JOIN pdd2.discount d2 
               WHERE pdd2.productDetail.id = p.id AND pdd2.status = 0 AND d2.status = 0 
               AND :time >= d2.startDate AND :time <= d2.endDate) as percentage
            , (SELECT d3.startDate FROM ProductDetailDiscount pdd3 JOIN pdd3.discount d3 
               WHERE pdd3.productDetail.id = p.id AND pdd3.status = 0 AND d3.status = 0 
               AND :time >= d3.startDate AND :time <= d3.endDate
               ORDER BY d3.percentage DESC, d3.id DESC LIMIT 1) as startDate
            , (SELECT d4.endDate FROM ProductDetailDiscount pdd4 JOIN pdd4.discount d4 
               WHERE pdd4.productDetail.id = p.id AND pdd4.status = 0 AND d4.status = 0 
               AND :time >= d4.startDate AND :time <= d4.endDate
               ORDER BY d4.percentage DESC, d4.id DESC LIMIT 1) as endDate

        FROM ProductDetail p
        WHERE (:#{#request.q} IS NULL OR p.name LIKE %:#{#request.q}% OR p.code LIKE %:#{#request.q}% OR p.product.name LIKE %:#{#request.q}%)
          AND (:#{#request.idGPU} IS NULL OR p.gpu.id IN :#{#request.idGPU})
          AND (:#{#request.idCPU} IS NULL OR p.cpu.id IN :#{#request.idCPU})
          AND (:#{#request.idColor} IS NULL OR p.color.id IN :#{#request.idColor})
          AND (:#{#request.idMaterial} IS NULL OR p.material.id IN :#{#request.idMaterial})
          AND (:#{#request.idHardDrive} IS NULL OR p.hardDrive.id IN :#{#request.idHardDrive})
          AND (:#{#request.idRAM} IS NULL OR p.ram.id IN :#{#request.idRAM})
          AND (:#{#request.idProduct} IS NULL OR p.product.id IN :#{#request.idProduct})
          AND (:#{#request.idBrand} IS NULL OR p.product.brand.id IN :#{#request.idBrand})
          AND (:#{#request.idScreen} IS NULL OR p.product.screen.id IN :#{#request.idScreen})
          AND (:#{#request.minPrice} IS NULL OR p.price >= :#{#request.minPrice})
          AND (:#{#request.maxPrice} IS NULL OR p.price <= :#{#request.maxPrice})
        GROUP BY p.id, p.code, p.name, p.description, p.price, p.status, p.urlImage, p.createdDate,
                 p.hardDrive.name, p.material.name, p.color.name, p.gpu.name, p.cpu.name, p.ram.name,
                 p.product.name, p.product.screen.name, p.product.brand.name, p.product.battery.name, p.product.operatingSystem.name
        ORDER BY p.createdDate DESC
        """)
    Page<ClientPDProductDetailResponse> getProductDetailsDiscount(
            Pageable pageable,
            @Param("request") ClientPDProductDetailRequest request,
            @Param("time") Long time
    );

    @Query(value = """
                    SELECT
                          p.id as id
                        , p.code as code
                        , p.name as name
                        , p.description as description
                        , p.hardDrive.id as idHardDrive
                        , p.material.id as idMaterial
                        , p.color.id as idColor
                        , p.gpu.id as idGPU
                        , p.cpu.id as idCPU
                        , p.product.id as idProduct
                        , p.price as price
                        , p.ram.id as idRAM
                        , p.urlImage as urlImage
                        , p.product.name as productName
                        , p.cpu.name as cpuName
                        , p.gpu.name as gpuName
                        , p.ram.name as ramName
                        , p.hardDrive.name as hardDriveName
                        , p.color.name as colorName
                        , p.product.screen.name as screenName
                        , p.material.name as materialName
                        , p.product.brand.name as brandName
                        , p.product.battery.name as batteryName
                        , p.product.operatingSystem.name as operatingSystemName
                        , MAX(
                          CASE
                            WHEN (pdd.id IS NOT NULL AND (d.startDate <= :time and :time <= d.endDate) AND pdd.status = 0 AND d.status = 0) THEN d.percentage
                            ELSE NULL
                          END
                        ) AS percentage
                        , MAX(
                          CASE
                            WHEN (pdd.id IS NOT NULL AND (d.startDate <= :time and :time <= d.endDate) AND pdd.status = 0 AND d.status = 0) THEN d.endDate
                            ELSE NULL
                          END
                        ) AS endDate
                    FROM ProductDetail p
                    LEFT JOIN ProductDetailDiscount pdd ON p.id = pdd.productDetail.id
                    LEFT JOIN Discount d ON pdd.discount.id = d.id
            
                    WHERE p.id = :id
            
                    GROUP BY
                        p.id, p.code, p.name, p.description,
                        p.hardDrive.id, p.material.id, p.color.id, 
                        p.gpu.id, p.cpu.id, p.product.id, p.price, 
                        p.ram.id, p.urlImage, 
                        p.product.name, p.cpu.name, p.ram.name, 
                        p.hardDrive.name, p.color.name
            """)
    Optional<ClientPDProductDetailDetailResponse> getProductById(@Param("id") String id, Long time);

    Optional<ProductDetail> findByCode(String code);

    @Query(value = """
            SELECT MIN(p.price) as priceMin, MAX(p.price) as priceMax
            FROM ProductDetail p
            """)
    Optional<ClientPDPriceMinMaxResponse> findPriceMinMax();

    Optional<ProductDetail> findByHardDriveAndMaterialAndColorAndGpuAndCpuAndRamAndProduct(HardDrive hardDrive, Material material, Color color, GPU gpu, CPU cpu, RAM ram, Product product);

    @Query(value = """
            SELECT
                pd.id
            FROM ProductDetail pd
            WHERE pd.product.id = :idProduct
                AND pd.hardDrive.id = (:#{#request.idHardDrive})
                AND pd.material.id = (:#{#request.idMaterial})
                AND pd.color.id = (:#{#request.idColor})
                AND pd.gpu.id = (:#{#request.idGPU})
                AND pd.cpu.id = (:#{#request.idCPU})
                AND pd.ram.id = (:#{#request.idRAM})
            """)
    Optional<String> checkExistByHardDriveAndMaterialAndColorAndGpuAndCpuAndRamAndProduct(String idProduct, ClientPDExistVariantRequest.PropertiesVariant request);
}