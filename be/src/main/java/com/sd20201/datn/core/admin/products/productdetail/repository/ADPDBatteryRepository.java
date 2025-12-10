package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.product.model.response.ADPRPropertyComboboxResponse;
import com.sd20201.datn.repository.BatteryRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPDBatteryRepository extends BatteryRepository {

    @Query(value = """
    SELECT
        b.id as value
        , b.name as label
    FROM Battery b
    ORDER BY b.createdDate desc
    """)
    List<ADPRPropertyComboboxResponse> getBatteryComboboxResponse();

}
