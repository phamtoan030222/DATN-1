package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.entity.Color;
import com.sd20201.datn.repository.ColorRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADPDColorRepository extends ColorRepository {

    @Query(value = """ 
    SELECT c.id as value, c.name as label, c.code as code FROM Color c
    ORDER BY c.createdDate desc
    """)
    List<ADPDPropertyComboboxResponse> getColors();


    @Query(value = """ 
        SELECT c as code FROM Color c
        WHERE LOWER(c.name) = LOWER(:name)
    """)
    Optional<Color> findByName(String name);

    Optional<Color> findByCode(String code);
}
