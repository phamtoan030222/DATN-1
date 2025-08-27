package com.sd20201.datn.core.admin.products.product.repository;

import com.sd20201.datn.core.admin.products.product.model.response.ADPRPropertyComboboxResponse;
import com.sd20201.datn.repository.ScreenRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPRScreenRepository extends ScreenRepository {

    @Query(value = """
    SELECT
        s.id as value
        , s.name as label
    FROM Screen s
    """)
    List<ADPRPropertyComboboxResponse> getScreenComboboxResponse();

}
