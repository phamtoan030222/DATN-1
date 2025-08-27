package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.repository.MaterialRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPDMaterialRepository extends MaterialRepository {

    @Query(value = """ 
    SELECT m.id as value, m.code as label FROM Material m
    """)
    List<ADPDPropertyComboboxResponse> getMaterials();

}
