package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.product.model.response.ADPRPropertyComboboxResponse;
import com.sd20201.datn.entity.Brand;
import com.sd20201.datn.repository.BrandRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADPDBrandRepository extends BrandRepository {

    @Query(value = """
    SELECT
        b.id as value
        , b.name as label
    FROM Brand b
    ORDER BY b.createdDate desc
    """)
    List<ADPRPropertyComboboxResponse> getBrandComboboxResponse();

    @Query(value = """ 
        SELECT b FROM Brand b
        WHERE LOWER(b.name) = LOWER(:name)
    """)
    Optional<Brand> findByName(String name);

    Optional<Brand> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

}
