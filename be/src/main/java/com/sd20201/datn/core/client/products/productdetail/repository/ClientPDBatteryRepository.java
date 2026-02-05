package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.product.model.response.ClientPRPropertyComboboxResponse;
import com.sd20201.datn.entity.Battery;
import com.sd20201.datn.repository.BatteryRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientPDBatteryRepository extends BatteryRepository {

    @Query(value = """
    SELECT
        b.id as value
        , b.name as label
    FROM Battery b
    ORDER BY b.name desc
    """)
    List<ClientPRPropertyComboboxResponse> getBatteryComboboxResponse();

    @Query(value = """ 
        SELECT b FROM Battery b
        WHERE LOWER(b.name) = LOWER(:name)
    """)
    Optional<Battery> findByName(String name);

}
