package com.sd20201.datn.core.admin.banhang.repository;

import com.sd20201.datn.core.admin.banhang.model.response.ADBHPropertyComboboxResponse;
import com.sd20201.datn.entity.GPU;
import com.sd20201.datn.repository.GPURepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADBHGPURepository extends GPURepository {

    @Query(value = """ 
    SELECT g.id as value, g.name as label FROM GPU g
    ORDER BY g.createdDate desc
    """)
    List<ADBHPropertyComboboxResponse> getGPUs();

    @Query(value = """ 
        SELECT g FROM GPU g
        WHERE LOWER(g.name) = LOWER(:name)
    """)
    Optional<GPU> findByName(String name);
}
