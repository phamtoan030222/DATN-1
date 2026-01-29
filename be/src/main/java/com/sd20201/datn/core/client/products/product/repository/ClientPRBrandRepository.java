package com.sd20201.datn.core.client.products.product.repository;

import com.sd20201.datn.core.client.products.product.model.response.ClientPRPropertyComboboxResponse;
import com.sd20201.datn.repository.BrandRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientPRBrandRepository extends BrandRepository {

    @Query(value = """
    SELECT
        b.id as value
        , b.name as label
    FROM Brand b
    ORDER BY b.createdDate desc
    """)
    List<ClientPRPropertyComboboxResponse> getBrandComboboxResponse();

}
