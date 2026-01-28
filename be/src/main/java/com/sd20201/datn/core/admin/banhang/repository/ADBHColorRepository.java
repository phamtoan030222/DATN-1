package com.sd20201.datn.core.admin.banhang.repository;

import com.sd20201.datn.core.admin.banhang.model.response.ADBHPropertyComboboxResponse;
import com.sd20201.datn.entity.Color;
import com.sd20201.datn.repository.ColorRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADBHColorRepository extends ColorRepository {

    @Query(value = """ 
    SELECT c.id as value, c.name as label, c.code as code FROM Color c
    ORDER BY c.createdDate desc
    """)
    List<ADBHPropertyComboboxResponse> getColors();


    @Query(value = """ 
        SELECT c as code FROM Color c
        WHERE LOWER(c.name) = LOWER(:name)
    """)
    Optional<Color> findByName(String name);

    Optional<Color> findByCode(String code);
}
