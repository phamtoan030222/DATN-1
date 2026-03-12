package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDPropertyComboboxResponse;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Color;
import com.sd20201.datn.repository.ColorRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientPDColorRepository extends ColorRepository {

    @Query(value = """ 
            SELECT c.id as value, c.name as label, c.code as code FROM Color c
            ORDER BY c.createdDate desc
            """)
    List<ClientPDPropertyComboboxResponse> getColors();


    @Query(value = """ 
                SELECT c as code FROM Color c
                WHERE LOWER(c.name) = LOWER(:name)
            """)
    Optional<Color> findByName(String name);

    Optional<Color> findByCode(String code);

    @Query(value = """ 
                SELECT DISTINCT c.id as value, c.name as label, c.code as code
                FROM ProductDetail pd
                JOIN pd.color c
                WHERE pd.product.id = (
                    SELECT pd2.product.id
                    FROM ProductDetail pd2
                    WHERE pd2.id = :pdId
                )
                ORDER BY c.name ASC
            """)
    List<ClientPDPropertyComboboxResponse> getColorByPD(@Param("pdId") String pdId);
}

