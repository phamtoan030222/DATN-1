package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.core.admin.products.productdetail.model.response.ADPDPropertyComboboxResponse;
import com.sd20201.datn.repository.ColorRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPDColorRepository extends ColorRepository {

    @Query(value = """ 
    SELECT c.id as value, c.name as label FROM Color c
    """)
    List<ADPDPropertyComboboxResponse> getColors();

}
