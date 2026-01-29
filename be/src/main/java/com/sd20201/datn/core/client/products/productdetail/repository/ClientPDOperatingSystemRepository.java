package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.product.model.response.ClientPRPropertyComboboxResponse;
import com.sd20201.datn.entity.OperatingSystem;
import com.sd20201.datn.repository.OperatingSystemRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientPDOperatingSystemRepository extends OperatingSystemRepository {

    @Query(value = """
    SELECT
        o.id as value
        , o.name as label
    FROM OperatingSystem o
    """)
    List<ClientPRPropertyComboboxResponse> getOperatingSystemComboboxResponse();

    @Query(value = """ 
        SELECT o FROM OperatingSystem o
        WHERE LOWER(o.name) = LOWER(:name)
    """)
    Optional<OperatingSystem> findByName(String name);
}
