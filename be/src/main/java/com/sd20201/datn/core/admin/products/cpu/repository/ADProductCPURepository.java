package com.sd20201.datn.core.admin.products.cpu.repository;

import com.sd20201.datn.core.admin.products.cpu.model.request.ADProductCPURequest;
import com.sd20201.datn.core.admin.products.cpu.model.response.ADProductCPUResponse;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.repository.CPURepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ADProductCPURepository extends CPURepository {

    @Query(value = """
    SELECT c
    FROM CPU c
    where
        (
            :#{#request.q} is null or c.name like concat('%',:#{#request.q},'%')
            OR :#{#request.q} is null or c.code like concat('%',:#{#request.q},'%')
        ) AND (:#{#request.brand} is NULL OR c.brand like concat('%',:#{#request.brand},'%'))
          AND (:#{#request.releaseYear} is NULL OR c.releaseYear = :#{#request.releaseYear})
          AND (:#{#request.series} is NULL OR c.series like concat('%',:#{#request.series},'%'))
          AND (:#{#request.generation} is NULL OR c.generation like concat('%',:#{#request.generation},'%'))
    ORDER BY c.createdDate DESC
    """, countQuery = """
    SELECT COUNT(1)
    FROM CPU c
    where
        (
            :#{#request.q} is null or c.name like concat('%',:#{#request.q},'%')
            OR :#{#request.q} is null or c.code like concat('%',:#{#request.q},'%')
        ) AND (:#{#request.brand} is NULL OR c.brand like concat('%',:#{#request.brand},'%'))
          AND (:#{#request.releaseYear} is NULL OR c.releaseYear = :#{#request.releaseYear})
          AND (:#{#request.series} is NULL OR c.series like concat('%',:#{#request.series},'%'))
          AND (:#{#request.generation} is NULL OR c.generation like concat('%',:#{#request.generation},'%'))
    """)
    Page<ADProductCPUResponse> getCPUs(Pageable pageable, ADProductCPURequest request);

    Optional<ADProductCPUResponse> getCPUById(String id);

    Optional<CPU> getCPUByCode(String code);
}
