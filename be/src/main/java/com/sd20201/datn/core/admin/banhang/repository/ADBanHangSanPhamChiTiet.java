package com.sd20201.datn.core.admin.banhang.repository;


import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDProductDetailResponse;
import com.sd20201.datn.repository.ProductDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ADBanHangSanPhamChiTiet extends ProductDetailRepository {

    @Query(value = """
    SELECT
        p.id as id,
        p.code as code,
        p.name as name,
        p.description as description,
        p.hardDrive.name as hardDrive,
        p.material.name as material,
        p.color.name as color,
        p.gpu.name as gpu,
        p.cpu.name as cpu,
        p.ram.name as ram,
        p.price as price,
        p.status as status,
        (SELECT COUNT(i.id) FROM IMEI i WHERE i.productDetail.id = p.id AND i.imeiStatus = 0) as quantity,
        p.urlImage as urlImage,
        d.percentage as percentage
    FROM ProductDetail p
    Join ProductDetailDiscount  pdd on p.id = pdd.productDetail.id
    Join Discount d on d.id = pdd.discount.id
    WHERE (
        :#{#request.q} IS NULL 
        OR p.name LIKE CONCAT('%', :#{#request.q}, '%') 
        OR p.code LIKE CONCAT('%', :#{#request.q}, '%')
    )
    AND (:#{#request.idGPU} IS NULL OR p.gpu.id = :#{#request.idGPU})
    AND (:#{#request.idCPU} IS NULL OR p.cpu.id = :#{#request.idCPU})
    AND (:#{#request.idColor} IS NULL OR p.color.id = :#{#request.idColor})
    AND (:#{#request.idMaterial} IS NULL OR p.material.id = :#{#request.idMaterial})
    AND (:#{#request.idHardDrive} IS NULL OR p.hardDrive.id = :#{#request.idHardDrive})
    AND (:#{#request.idRAM} IS NULL OR p.ram.id = :#{#request.idRAM})
    AND (:#{#request.idProduct} IS NULL OR p.product.id = :#{#request.idProduct})
    AND (:#{#request.minPrice} IS NULL OR p.price >= :#{#request.minPrice})
    AND (:#{#request.maxPrice} IS NULL OR p.price <= :#{#request.maxPrice})
    AND p.status = 0 
    AND d.status = 0
    AND pdd.status = 0
    ORDER BY p.createdDate DESC
    """, countQuery = """
    SELECT COUNT(p.id)
    FROM ProductDetail p
    WHERE (
        :#{#request.q} IS NULL 
        OR p.name LIKE CONCAT('%', :#{#request.q}, '%') 
        OR p.code LIKE CONCAT('%', :#{#request.q}, '%')
    )
    AND (:#{#request.idGPU} IS NULL OR p.gpu.id = :#{#request.idGPU})
    AND (:#{#request.idCPU} IS NULL OR p.cpu.id = :#{#request.idCPU})
    AND (:#{#request.idColor} IS NULL OR p.color.id = :#{#request.idColor})
    AND (:#{#request.idMaterial} IS NULL OR p.material.id = :#{#request.idMaterial})
    AND (:#{#request.idHardDrive} IS NULL OR p.hardDrive.id = :#{#request.idHardDrive})
    AND (:#{#request.idRAM} IS NULL OR p.ram.id = :#{#request.idRAM})
    AND (:#{#request.idProduct} IS NULL OR p.product.id = :#{#request.idProduct})
    AND (:#{#request.minPrice} IS NULL OR p.price >= :#{#request.minPrice})
    AND (:#{#request.maxPrice} IS NULL OR p.price <= :#{#request.maxPrice})
        AND p.status = 0
    """)
    Page<ADPDProductDetailResponse> getProductDetails(Pageable pageable, @Param("request") ADPDProductDetailRequest request);

}
