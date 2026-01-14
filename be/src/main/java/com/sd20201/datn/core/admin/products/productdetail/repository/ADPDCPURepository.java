package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.repository.CPURepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADPDCPURepository extends CPURepository {

    @Query(value = """ 
    SELECT c.id as value, c.name as label FROM CPU c
    ORDER BY c.createdDate desc
    """)
    List<ADPDPropertyComboboxResponse> getCPUs();

    @Query(value = """ 
        SELECT c FROM CPU c
        WHERE LOWER(c.name) = LOWER(:name)
    """)
    Optional<CPU> findByName(String name);

}
