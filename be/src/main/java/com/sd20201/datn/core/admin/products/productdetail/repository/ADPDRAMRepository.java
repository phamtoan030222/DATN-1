package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.repository.RAMRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPDRAMRepository extends RAMRepository {

    @Query(value = """ 
    SELECT r.id as value, r.name as label FROM RAM r
    """)
    List<ADPDPropertyComboboxResponse> getRAMs();

}
