package com.sd20201.datn.core.client.products.product.repository;

import com.sd20201.datn.core.client.products.product.model.response.ClientPRPropertyComboboxResponse;
import com.sd20201.datn.repository.OperatingSystemRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientPROperatingSystemRepository extends OperatingSystemRepository {

    @Query(value = """
    SELECT
        o.id as value
        , o.name as label
    FROM OperatingSystem o
    ORDER BY o.createdDate desc
    """)
    List<ClientPRPropertyComboboxResponse> getOperatingSystemComboboxResponse();

}
