package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.repository.GPURepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPDGPURepository extends GPURepository {

    @Query(value = """ 
    SELECT g.id as value, g.name as label FROM GPU g
    """)
    List<ADPDPropertyComboboxResponse> getGPUs();
}
