package com.sd20201.datn.core.admin.products.product.repository;

import com.sd20201.datn.core.admin.products.product.model.response.ADPRPropertyComboboxResponse;
import com.sd20201.datn.repository.BatteryRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPRBatteryRepository extends BatteryRepository {

    @Query(value = """
    SELECT
        b.id as value
        , b.name as label
    FROM Battery b
    """)
    List<ADPRPropertyComboboxResponse> getBatteryComboboxResponse();

}
