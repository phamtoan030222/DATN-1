package com.sd20201.datn.core.client.products.product.repository;

import com.sd20201.datn.core.client.products.product.model.response.ClientPRPropertyComboboxResponse;
import com.sd20201.datn.repository.ScreenRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientPRScreenRepository extends ScreenRepository {

    @Query(value = """
    SELECT
        s.id as value
        , s.name as label
    FROM Screen s
    ORDER BY s.createdDate desc
    """)
    List<ClientPRPropertyComboboxResponse> getScreenComboboxResponse();

}
