package com.sd20201.datn.core.admin.products.screen.repository;

import com.sd20201.datn.core.admin.products.screen.model.request.ADProductScreenRequest;
import com.sd20201.datn.core.admin.products.screen.model.response.ADProductScreenDetailResponse;
import com.sd20201.datn.core.admin.products.screen.model.response.ADProductScreenResponse;
import com.sd20201.datn.entity.Screen;
import com.sd20201.datn.repository.ScreenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ADProductScreenRepository extends ScreenRepository {

    @Query(value = """
    SELECT
            s.id as id
            , s.code as code
            , s.name as name
            , s.physicalSize as physicalSize
            , s.resolution as resolution
            , s.panelType as panelType
            , s.technology as technology
    FROM Screen s
    where
        (
            :#{#request.q} is null or s.name like concat('%',:#{#request.q},'%')
            OR :#{#request.q} is null or s.code like concat('%',:#{#request.q},'%')
        ) AND (:#{#request.technology} is NULL OR s.technology like concat('%',:#{#request.technology},'%'))
          AND (:#{#request.resolution} is NULL OR s.resolution = :#{#request.resolution})
          AND (:#{#request.physicalSize} is NULL OR s.physicalSize = :#{#request.physicalSize})
          AND (:#{#request.panelType} is NULL OR s.panelType like concat('%',:#{#request.panelType},'%'))
    ORDER BY s.createdDate DESC
    """, countQuery = """
    SELECT COUNT(1)
    FROM Screen s
    where
        (
            :#{#request.q} is null or s.name like concat('%',:#{#request.q},'%')
            OR :#{#request.q} is null or s.code like concat('%',:#{#request.q},'%')
        ) AND (:#{#request.technology} is NULL OR s.technology like concat('%',:#{#request.technology},'%'))
          AND (:#{#request.resolution} is NULL OR s.resolution = :#{#request.resolution})
          AND (:#{#request.physicalSize} is NULL OR s.physicalSize = :#{#request.physicalSize})
          AND (:#{#request.panelType} is NULL OR s.panelType like concat('%',:#{#request.panelType},'%'))
    """)
    Page<ADProductScreenResponse> getScreens(Pageable pageable, ADProductScreenRequest request);

    @Query(value = """
    SELECT
            s.id as id
            , s.code as code
            , s.name as name
            , s.physicalSize as physicalSize
            , s.resolution as resolution
            , s.panelType as panelType
            , s.technology as technology
    FROM Screen s
    where s.id = :id
    """)
    Optional<ADProductScreenDetailResponse> getScreenById(@Param("id") String id);

    Optional<Screen> findByCode(String code);

}
