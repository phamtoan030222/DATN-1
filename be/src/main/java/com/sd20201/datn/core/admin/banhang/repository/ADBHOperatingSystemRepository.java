package com.sd20201.datn.core.admin.banhang.repository;

import com.sd20201.datn.core.admin.banhang.model.response.ADBHPropertyComboboxResponse;
import com.sd20201.datn.entity.OperatingSystem;
import com.sd20201.datn.repository.OperatingSystemRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADBHOperatingSystemRepository extends OperatingSystemRepository {

    @Query(value = """
    SELECT
        o.id as value
        , o.name as label
    FROM OperatingSystem o
    """)
    List<ADBHPropertyComboboxResponse> getOperatingSystemComboboxResponse();

    @Query(value = """ 
        SELECT o FROM OperatingSystem o
        WHERE LOWER(o.name) = LOWER(:name)
    """)
    Optional<OperatingSystem> findByName(String name);
}
