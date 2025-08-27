package com.sd20201.datn.core.admin.products.screen.repository;

import com.sd20201.datn.core.admin.products.screen.model.response.ADProductScreenResolutionResponse;
import com.sd20201.datn.repository.ScreenResolutionRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADProductScreenResolutionRepository extends ScreenResolutionRepository {

    @Query(value = """
    SELECT
        sr.id AS id,
        sr.code AS code,
        sr.name AS name
    FROM ScreenResolution sr
    """)
    List<ADProductScreenResolutionResponse> getScreenResolutions();

}
