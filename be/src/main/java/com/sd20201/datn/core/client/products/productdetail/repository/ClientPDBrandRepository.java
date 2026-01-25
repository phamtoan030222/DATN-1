package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.product.model.response.ClientPRPropertyComboboxResponse;
import com.sd20201.datn.entity.Brand;
import com.sd20201.datn.repository.BrandRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientPDBrandRepository extends BrandRepository {

    @Query(value = """
    SELECT
        b.id as value
        , b.name as label
    FROM Brand b
    ORDER BY b.createdDate desc
    """)
    List<ClientPRPropertyComboboxResponse> getBrandComboboxResponse();

    @Query(value = """ 
        SELECT b FROM Brand b
        WHERE LOWER(b.name) = LOWER(:name)
    """)
    Optional<Brand> findByName(String name);

    Optional<Brand> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

}
