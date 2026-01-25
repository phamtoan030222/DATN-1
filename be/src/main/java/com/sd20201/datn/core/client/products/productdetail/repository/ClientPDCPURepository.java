package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDPropertyComboboxResponse;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.repository.CPURepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientPDCPURepository extends CPURepository {

    @Query(value = """ 
    SELECT c.id as value, c.name as label FROM CPU c
    ORDER BY c.createdDate desc
    """)
    List<ClientPDPropertyComboboxResponse> getCPUs();

    @Query(value = """ 
        SELECT c FROM CPU c
        WHERE LOWER(c.name) = LOWER(:name)
    """)
    Optional<CPU> findByName(String name);

}
