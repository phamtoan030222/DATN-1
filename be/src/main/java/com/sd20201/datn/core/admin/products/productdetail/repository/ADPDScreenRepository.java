package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.product.model.response.ADPRPropertyComboboxResponse;
import com.sd20201.datn.repository.ScreenRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPDScreenRepository extends ScreenRepository {

    @Query(value = """
    SELECT
        s.id as value
        , s.name as label
    FROM Screen s
    ORDER BY s.createdDate desc
    """)
    List<ADPRPropertyComboboxResponse> getScreenComboboxResponse();

}
