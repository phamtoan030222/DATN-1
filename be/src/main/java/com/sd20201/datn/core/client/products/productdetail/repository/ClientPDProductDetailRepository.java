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

    // Query thường (Không có giảm giá) - Giữ nguyên logic của bạn, chỉ chuẩn hóa format
    @Query(value = """
            SELECT
                    p.id as id
                    , p.code as code
                    , p.name as name
                    , p.description as description
                    , p.hardDrive.name as hardDrive
                    , p.material.name as material
                    , p.color.name as color
                    , p.gpu.name as gpu
                    , p.cpu.name as cpu
                    , p.ram.name as ram
                    , p.price as price
                    , p.status as status
                    , (SELECT COUNT(i.id) FROM IMEI i WHERE i.productDetail.id = p.id AND (i.imeiStatus = 0 OR i.imeiStatus = 1)) as quantity
                    , p.urlImage as urlImage
                    , p.product.name as productName
                    , p.product.screen.name as screenName
                    , p.product.brand.name as brandName
                    , p.product.battery.name as batteryName
                    , p.product.operatingSystem.name as operatingSystemName
            FROM ProductDetail p
            WHERE
                (
                    :#{#request.q} is null or p.name like concat('%',:#{#request.q},'%')
                    OR :#{#request.q} is null or p.code like concat('%',:#{#request.q},'%')
                ) AND (:#{#request.idGPU} is NULL OR p.gpu.id like concat('%',:#{#request.idGPU},'%'))
                  AND (:#{#request.idCPU} is NULL OR p.cpu.id like concat('%',:#{#request.idCPU},'%'))
                  AND (:#{#request.idColor} is NULL OR p.color.id like concat('%',:#{#request.idColor},'%'))
                  AND (:#{#request.idMaterial} is NULL OR p.material.id like concat('%',:#{#request.idMaterial},'%'))
                  AND (:#{#request.idHardDrive} is NULL OR p.hardDrive.id like concat('%',:#{#request.idHardDrive},'%'))
                  AND (:#{#request.idRAM} is NULL OR p.ram.id like concat('%',:#{#request.idRAM},'%'))
                  AND (:#{#request.idProduct} is NULL OR p.product.id like concat('%',:#{#request.idProduct},'%'))
                  AND (:#{#request.minPrice} IS NULL OR p.price >= :#{#request.minPrice})
                  AND (:#{#request.maxPrice} IS NULL OR p.price <= :#{#request.maxPrice})
            
            GROUP BY
                     p.id, p.code, p.name, p.description, p.price, p.status, p.urlImage, p.createdDate,
                     p.hardDrive.name, p.material.name, p.color.name,
                     p.gpu.name, p.cpu.name, p.ram.name,
                     p.product.name, p.product.screen.name,
                     p.product.brand.name, p.product.battery.name,
                     p.product.operatingSystem.name
            ORDER BY p.createdDate DESC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT p.id)
                    FROM ProductDetail p
                    WHERE
                        (
                            :#{#request.q} is null or p.name like concat('%',:#{#request.q},'%')
                            OR :#{#request.q} is null or p.code like concat('%',:#{#request.q},'%')
                        ) AND (:#{#request.idGPU} is NULL OR p.gpu.id like concat('%',:#{#request.idGPU},'%'))
                          AND (:#{#request.idCPU} is NULL OR p.cpu.id like concat('%',:#{#request.idCPU},'%'))
                          AND (:#{#request.idColor} is NULL OR p.color.id like concat('%',:#{#request.idColor},'%'))
                          AND (:#{#request.idMaterial} is NULL OR p.material.id like concat('%',:#{#request.idMaterial},'%'))
                          AND (:#{#request.idHardDrive} is NULL OR p.hardDrive.id like concat('%',:#{#request.idHardDrive},'%'))
                          AND (:#{#request.idRAM} is NULL OR p.ram.id like concat('%',:#{#request.idRAM},'%'))
                          AND (:#{#request.idProduct} is NULL OR p.product.id like concat('%',:#{#request.idProduct},'%'))
                          AND (:#{#request.minPrice} IS NULL OR p.price >= :#{#request.minPrice})
                          AND (:#{#request.maxPrice} IS NULL OR p.price <= :#{#request.maxPrice})
                    """)
    Page<ClientPDProductDetailResponse> getProductDetails(Pageable pageable, ClientPDProductDetailRequest request);

    // ==================================================================================================
    // [QUAN TRỌNG] Query có Discount - Đã fix GROUP BY đầy đủ
    // ==================================================================================================
    @Query(value = """
            SELECT
                 p.id as id
                , p.code as code
                , p.name as name
                , p.hardDrive.name as hardDrive
                , p.material.name as material
                , p.color.name as color
                , p.gpu.name as gpu
                , p.cpu.name as cpu
                , p.ram.name as ram
                , p.price as price
                , p.status as status
                , (SELECT COUNT(i.id) FROM IMEI i WHERE i.productDetail.id = p.id AND (i.imeiStatus = 0 OR i.imeiStatus = 1)) as quantity
                , p.urlImage as urlImage
                , p.product.name as productName
                , p.product.screen.name as screenName
                , p.product.brand.name as brandName
                , p.product.battery.name as batteryName
                , p.product.operatingSystem.name as operatingSystemName
                
                , MAX(d.percentage) as percentage
                , MAX(d.endDate) as endDate
            FROM ProductDetail p
                LEFT JOIN ProductDetailDiscount pdd ON p.id = pdd.productDetail.id
                LEFT JOIN Discount d ON pdd.discount.id = d.id AND d.endDate >= :currentTime
            
            WHERE
                (
                    :#{#request.q} IS NULL OR p.name LIKE CONCAT('%',:#{#request.q},'%')
                    OR :#{#request.q} IS NULL OR p.code LIKE CONCAT('%',:#{#request.q},'%')
                ) 
                AND (:#{#request.idGPU} IS NULL OR p.gpu.id LIKE CONCAT('%',:#{#request.idGPU},'%'))
                AND (:#{#request.idCPU} IS NULL OR p.cpu.id LIKE CONCAT('%',:#{#request.idCPU},'%'))
                AND (:#{#request.idColor} IS NULL OR p.color.id LIKE CONCAT('%',:#{#request.idColor},'%'))
                AND (:#{#request.idMaterial} IS NULL OR p.material.id LIKE CONCAT('%',:#{#request.idMaterial},'%'))
                AND (:#{#request.idHardDrive} IS NULL OR p.hardDrive.id LIKE CONCAT('%',:#{#request.idHardDrive},'%'))
                AND (:#{#request.idRAM} IS NULL OR p.ram.id LIKE CONCAT('%',:#{#request.idRAM},'%'))
                AND (:#{#request.idProduct} IS NULL OR p.product.id LIKE CONCAT('%',:#{#request.idProduct},'%'))
                AND ( pdd.id IN :idProductDetailDiscount OR pdd.id IS NULL)
                AND (
                    d.id IS NULL
                    OR d.percentage = (
                        SELECT MAX(subD.percentage)
                        FROM ProductDetailDiscount subPdd
                        JOIN subPdd.discount subD
                        WHERE subPdd.productDetail.id = p.id
                        AND subD.endDate >= :currentTime
                    )
                )
            
            GROUP BY 
                 p.id, p.code, p.name, p.price, p.status, p.urlImage, p.createdDate,
                 p.hardDrive.name, p.material.name, p.color.name,
                 p.gpu.name, p.cpu.name, p.ram.name,
                 p.product.name, 
                 p.product.screen.name,
                 p.product.brand.name, 
                 p.product.battery.name,
                 p.product.operatingSystem.name
            
            HAVING (:#{#request.minPrice} <= MIN(p.price) AND MAX(p.price) <= :#{#request.maxPrice})
            ORDER BY p.createdDate DESC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT p.id)
                    FROM ProductDetail p
                        LEFT JOIN ProductDetailDiscount pdd ON p.id = pdd.productDetail.id
                        LEFT JOIN Discount d ON pdd.discount.id = d.id AND d.endDate >= :currentTime
                    WHERE
                        (
                            :#{#request.q} IS NULL OR p.name LIKE CONCAT('%',:#{#request.q},'%')
                            OR :#{#request.q} IS NULL OR p.code LIKE CONCAT('%',:#{#request.q},'%')
                        ) 
                        AND (:#{#request.idGPU} IS NULL OR p.gpu.id LIKE CONCAT('%',:#{#request.idGPU},'%'))
                        AND (:#{#request.idCPU} IS NULL OR p.cpu.id LIKE CONCAT('%',:#{#request.idCPU},'%'))
                        AND (:#{#request.idColor} IS NULL OR p.color.id LIKE CONCAT('%',:#{#request.idColor},'%'))
                        AND (:#{#request.idMaterial} IS NULL OR p.material.id LIKE CONCAT('%',:#{#request.idMaterial},'%'))
                        AND (:#{#request.idHardDrive} IS NULL OR p.hardDrive.id LIKE CONCAT('%',:#{#request.idHardDrive},'%'))
                        AND (:#{#request.idRAM} IS NULL OR p.ram.id LIKE CONCAT('%',:#{#request.idRAM},'%'))
                        AND (:#{#request.idProduct} IS NULL OR p.product.id LIKE CONCAT('%',:#{#request.idProduct},'%'))
                        AND ( pdd.id IN :idProductDetailDiscount OR pdd.id IS NULL)
                    
                        AND (
                            d.id IS NULL
                            OR d.percentage = (
                                SELECT MAX(subD.percentage)
                                FROM ProductDetailDiscount subPdd
                                JOIN subPdd.discount subD
                                WHERE subPdd.productDetail.id = p.id
                                AND subD.endDate >= :currentTime
                            )
                        )
                    """)
    Page<ClientPDProductDetailResponse> getProductDetailsDiscount(
            Pageable pageable,
            ClientPDProductDetailRequest request,
            List<String> idProductDetailDiscount,
            @Param("currentTime") Long currentTime
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
                        , MAX(d.percentage) as percentage
                        , MAX(d.endDate) as endDate
                    FROM ProductDetail p
                    LEFT JOIN ProductDetailDiscount pdd ON p.id = pdd.productDetail.id
                    LEFT JOIN Discount d ON pdd.discount.id = d.id
            
                    WHERE p.id = :id
                      AND (
                          d.id IS NULL 
                          OR d.percentage = (
                              SELECT MAX(subD.percentage)
                              FROM ProductDetailDiscount subPdd
                              JOIN subPdd.discount subD
                              WHERE subPdd.productDetail.id = p.id
                          )
                      )
            
                    GROUP BY
                        p.id, p.code, p.name, p.description, 
                        p.hardDrive.id, p.material.id, p.color.id, 
                        p.gpu.id, p.cpu.id, p.product.id, p.price, 
                        p.ram.id, p.urlImage, 
                        p.product.name, p.cpu.name, p.ram.name, 
                        p.hardDrive.name, p.color.name
            """)
    Optional<ClientPDProductDetailDetailResponse> getProductById(@Param("id") String id);

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