package com.sd20201.datn.core.admin.products.product.repository;

import com.sd20201.datn.core.admin.products.product.model.response.ADPRPropertyComboboxResponse;
import com.sd20201.datn.repository.OperatingSystemRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPROperatingSystemRepository extends OperatingSystemRepository {

    @Query(value = """
    SELECT
        o.id as value
        , o.name as label
    FROM OperatingSystem o
    """)
    List<ADPRPropertyComboboxResponse> getOperatingSystemComboboxResponse();

}
