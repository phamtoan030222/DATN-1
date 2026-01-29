package com.sd20201.datn.core.admin.banhang.repository;

import com.sd20201.datn.core.admin.banhang.model.response.ADBHPropertyComboboxResponse;
import com.sd20201.datn.entity.Battery;
import com.sd20201.datn.repository.BatteryRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADBHBatteryRepository extends BatteryRepository {

    @Query(value = """
    SELECT
        b.id as value
        , b.name as label
    FROM Battery b
    ORDER BY b.createdDate desc
    """)
    List<ADBHPropertyComboboxResponse> getBatteryComboboxResponse();

    @Query(value = """ 
        SELECT b FROM Battery b
        WHERE LOWER(b.name) = LOWER(:name)
    """)
    Optional<Battery> findByName(String name);

}
