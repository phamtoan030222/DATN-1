package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDExistVariantRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPriceMinMaxResponse;
import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDProductDetailDetailResponse;
import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDProductDetailResponse;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.entity.Color;
import com.sd20201.datn.entity.GPU;
import com.sd20201.datn.entity.HardDrive;
import com.sd20201.datn.entity.Material;
import com.sd20201.datn.entity.Product;
import com.sd20201.datn.entity.ProductDetail;
import com.sd20201.datn.entity.RAM;
import com.sd20201.datn.repository.ProductDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ADPDProductDetailRepository extends ProductDetailRepository {

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
    FROM ProductDetail p
    where
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
    GROUP BY     p.id,
                 p.code,
                 p.name,
                 p.description,
                 p.hardDrive,
                 p.material,
                 p.color,
                 p.gpu,
                 p.cpu,
                 p.ram,
                 p.price,
                 p.status,
                 p.urlImage
    HAVING (:#{#request.minPrice} <= MIN(p.price) AND MAX(p.price) <= :#{#request.maxPrice})
    ORDER BY p.createdDate DESC
    """, countQuery = """
    SELECT
        COUNT(p.id)
    FROM ProductDetail p
    where
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
        GROUP BY p.id,
                 p.code,
                 p.name,
                 p.description,
                 p.hardDrive,
                 p.material,
                 p.color,
                 p.gpu,
                 p.cpu,
                 p.ram,
                 p.price,
                 p.status,
                 p.urlImage
       HAVING (:#{#request.minPrice} <= MIN(p.price) AND MAX(p.price) <= :#{#request.maxPrice})
    """)
    Page<ADPDProductDetailResponse> getProductDetails(Pageable pageable, ADPDProductDetailRequest request);

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
                        , MAX(d.percentage) as percentage
            FROM ProductDetail p
                LEFT join ProductDetailDiscount pdd on p.id = pdd.productDetail.id
                LEFT JOIN Discount d on pdd.discount.id = d.id
            where
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
                  AND ( pdd.id IN :idProductDetailDiscount OR pdd.id IS NULL)
            GROUP BY     p.id,
                         p.code,
                         p.name,
                         p.hardDrive,
                         p.material,
                         p.color,
                         p.gpu,
                         p.cpu,
                         p.ram,
                         p.price,
                         p.status,
                         p.urlImage
            HAVING (:#{#request.minPrice} <= MIN(p.price) AND MAX(p.price) <= :#{#request.maxPrice})
            ORDER BY p.createdDate DESC
            """, countQuery = """
            SELECT
                COUNT (p.id)
            FROM ProductDetail p
                LEFT join ProductDetailDiscount pdd on p.id = pdd.productDetail.id
                LEFT JOIN Discount d on pdd.discount.id = d.id
            where
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
                  AND ( pdd.id IN :idProductDetailDiscount OR pdd.id IS NULL)
            GROUP BY     p.id,
                         p.code,
                         p.name,
                         p.hardDrive,
                         p.material,
                         p.color,
                         p.gpu,
                         p.cpu,
                         p.ram,
                         p.price,
                         p.status,
                         p.urlImage
            HAVING (:#{#request.minPrice} <= MIN(p.price) AND MAX(p.price) <= :#{#request.maxPrice})
    """)
    Page<ADPDProductDetailResponse> getProductDetailsDiscount(Pageable pageable, ADPDProductDetailRequest request, List<String> idProductDetailDiscount);

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
    FROM ProductDetail p
    WHERE id = :id
    """)
    Optional<ADPDProductDetailDetailResponse> getProductById(@Param("id") String id);

    Optional<ProductDetail> findByCode(String code);

    @Query(value = """
    SELECT MIN(p.price) as priceMin, MAX(p.price) as priceMax
    FROM ProductDetail p
    """)
    Optional<ADPDPriceMinMaxResponse> findPriceMinMax();

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
    Optional<String> checkExistByHardDriveAndMaterialAndColorAndGpuAndCpuAndRamAndProduct(String idProduct, ADPDExistVariantRequest.PropertiesVariant request);
}
