package com.sd20201.datn.core.admin.products.cpu.repository;

import com.sd20201.datn.core.admin.products.cpu.model.request.ADProductCPURequest;
import com.sd20201.datn.core.admin.products.cpu.model.response.ADProductCPUResponse;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.repository.CPURepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository; // Thêm annotation này cho chuẩn

import java.util.Optional;

@Repository
public interface ADProductCPURepository extends CPURepository {

    @Query(value = """
    SELECT c
    FROM CPU c
    WHERE
        (
            :#{#request.q} IS NULL OR :#{#request.q} = ''
            OR LOWER(c.name) LIKE LOWER(CONCAT('%', :#{#request.q}, '%'))
            OR LOWER(c.code) LIKE LOWER(CONCAT('%', :#{#request.q}, '%'))
        )
        AND (:#{#request.brand} IS NULL OR :#{#request.brand} = '' OR c.brand LIKE CONCAT('%', :#{#request.brand}, '%'))
        AND (:#{#request.releaseYear} IS NULL OR c.releaseYear = :#{#request.releaseYear})
        AND (:#{#request.series} IS NULL OR :#{#request.series} = '' OR c.series LIKE CONCAT('%', :#{#request.series}, '%'))
        AND (:#{#request.generation} IS NULL OR :#{#request.generation} = '' OR c.generation LIKE CONCAT('%', :#{#request.generation}, '%'))
        AND (:#{#request.status} IS NULL OR c.status = :#{#request.status})
    ORDER BY c.createdDate DESC
    """, countQuery = """
    SELECT COUNT(c)
    FROM CPU c
    WHERE
        (
            :#{#request.q} IS NULL OR :#{#request.q} = ''
            OR LOWER(c.name) LIKE LOWER(CONCAT('%', :#{#request.q}, '%'))
            OR LOWER(c.code) LIKE LOWER(CONCAT('%', :#{#request.q}, '%'))
        )
        AND (:#{#request.brand} IS NULL OR :#{#request.brand} = '' OR c.brand LIKE CONCAT('%', :#{#request.brand}, '%'))
        AND (:#{#request.releaseYear} IS NULL OR c.releaseYear = :#{#request.releaseYear})
        AND (:#{#request.series} IS NULL OR :#{#request.series} = '' OR c.series LIKE CONCAT('%', :#{#request.series}, '%'))
        AND (:#{#request.generation} IS NULL OR :#{#request.generation} = '' OR c.generation LIKE CONCAT('%', :#{#request.generation}, '%'))
        AND (:#{#request.status} IS NULL OR c.status = :#{#request.status})
    """)
    Page<ADProductCPUResponse> getCPUs(Pageable pageable, ADProductCPURequest request);

    Optional<ADProductCPUResponse> getCPUById(String id);

    Optional<CPU> getCPUByCode(String code);
}