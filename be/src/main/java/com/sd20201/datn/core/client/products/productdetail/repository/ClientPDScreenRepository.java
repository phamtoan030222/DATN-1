package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.product.model.response.ClientPRPropertyComboboxResponse;
import com.sd20201.datn.entity.Screen;
import com.sd20201.datn.repository.ScreenRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientPDScreenRepository extends ScreenRepository {

    @Query(value = """
    SELECT
        s.id as value
        , s.name as label
    FROM Screen s
    ORDER BY s.createdDate desc
    """)
    List<ClientPRPropertyComboboxResponse> getScreenComboboxResponse();

    @Query(value = """ 
        SELECT s FROM Screen s
        WHERE LOWER(s.name) = LOWER(:name)
    """)
    Optional<Screen> findByName(String name);
}
