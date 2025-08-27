package com.sd20201.datn.core.admin.products.product.repository;

import com.sd20201.datn.core.admin.products.product.model.response.ADPRPropertyComboboxResponse;
import com.sd20201.datn.repository.BrandRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPRBrandRepository extends BrandRepository {

    @Query(value = """
    SELECT
        b.id as value
        , b.name as label
    FROM Brand b
    """)
    List<ADPRPropertyComboboxResponse> getBrandComboboxResponse();

}
