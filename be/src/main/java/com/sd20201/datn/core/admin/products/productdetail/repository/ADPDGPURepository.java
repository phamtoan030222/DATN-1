package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.entity.GPU;
import com.sd20201.datn.repository.GPURepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADPDGPURepository extends GPURepository {

    @Query(value = """ 
    SELECT g.id as value, g.name as label FROM GPU g
    ORDER BY g.createdDate desc
    """)
    List<ADPDPropertyComboboxResponse> getGPUs();

    @Query(value = """ 
        SELECT g FROM GPU g
        WHERE LOWER(g.name) = LOWER(:name)
    """)
    Optional<GPU> findByName(String name);
}
