package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.entity.Material;
import com.sd20201.datn.repository.MaterialRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADPDMaterialRepository extends MaterialRepository {

    @Query(value = """ 
    SELECT m.id as value, m.name as label FROM Material m
    ORDER BY m.createdDate desc
    """)
    List<ADPDPropertyComboboxResponse> getMaterials();

    @Query(value = """ 
        SELECT m FROM Material m
        WHERE LOWER(m.name) = LOWER(:name)
    """)
    Optional<Material> findByName(String name);
}
