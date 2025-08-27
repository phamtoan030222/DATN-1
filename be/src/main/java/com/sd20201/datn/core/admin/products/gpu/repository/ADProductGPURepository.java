package com.sd20201.datn.core.admin.products.gpu.repository;

import com.sd20201.datn.core.admin.products.gpu.model.request.ADProductGPURequest;
import com.sd20201.datn.core.admin.products.gpu.model.response.ADProductGPUResponse;
import com.sd20201.datn.repository.GPURepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADProductGPURepository extends GPURepository {

    @Query(value = """
    SELECT g
    FROM GPU g
    where
        (
            :#{#request.q} is null or g.name like concat('%',:#{#request.q},'%')
            OR :#{#request.q} is null or g.code like concat('%',:#{#request.q},'%')
        ) AND (:#{#request.brand} is NULL OR g.brand like concat('%',:#{#request.brand},'%'))
          AND (:#{#request.releaseYear} is NULL OR g.releaseYear = :#{#request.releaseYear})
          AND (:#{#request.series} is NULL OR g.series like concat('%',:#{#request.series},'%'))
          AND (:#{#request.generation} is NULL OR g.generation like concat('%',:#{#request.generation},'%'))
    ORDER BY g.createdDate DESC
    """, countQuery = """
    SELECT COUNT(1)
    FROM GPU g
    where
        (
            :#{#request.q} is null or g.name like concat('%',:#{#request.q},'%')
            OR :#{#request.q} is null or g.code like concat('%',:#{#request.q},'%')
        ) AND (:#{#request.brand} is NULL OR g.brand like concat('%',:#{#request.brand},'%'))
          AND (:#{#request.releaseYear} is NULL OR g.releaseYear = :#{#request.releaseYear})
          AND (:#{#request.series} is NULL OR g.series like concat('%',:#{#request.series},'%'))
          AND (:#{#request.generation} is NULL OR g.generation like concat('%',:#{#request.generation},'%'))
    """)
    Page<ADProductGPUResponse> getGPUs(Pageable pageable, ADProductGPURequest request);

    Optional<ADProductGPUResponse> getGPUById(String id);

}
