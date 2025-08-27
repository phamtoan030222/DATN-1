package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.repository.CPURepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPDCPURepository extends CPURepository {

    @Query(value = """ 
    SELECT c.id as value, c.name as label FROM CPU c
    """)
    List<ADPDPropertyComboboxResponse> getCPUs();

}
