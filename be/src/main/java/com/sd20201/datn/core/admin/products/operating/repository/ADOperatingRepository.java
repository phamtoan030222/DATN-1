package com.sd20201.datn.core.admin.products.operating.repository;

import com.sd20201.datn.core.admin.products.operating.model.response.ADOperatingResponse;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.repository.OperatingSystemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ADOperatingRepository extends OperatingSystemRepository {

    @Query(
            value = """
                        SELECT os.id AS id,
                               os.name AS name,
                               os.code AS code,
                               os.version AS version,
                               os.description AS description,
                               os.status AS status
                        FROM OperatingSystem os
                        WHERE 
                        (
                            :key IS NULL OR :key = ''
                            OR LOWER(os.name) LIKE LOWER(CONCAT('%', :key, '%')) 
                            OR LOWER(os.code) LIKE LOWER(CONCAT('%', :key, '%')) 
                            OR LOWER(os.version) LIKE LOWER(CONCAT('%', :key, '%'))
                        )
                        AND (:status IS NULL OR os.status = :status)
                        ORDER BY os.createdDate DESC
                    """,
            countQuery = """
                        SELECT COUNT(os.id)
                        FROM OperatingSystem os
                        WHERE 
                        (
                            :key IS NULL OR :key = ''
                            OR LOWER(os.name) LIKE LOWER(CONCAT('%', :key, '%')) 
                            OR LOWER(os.code) LIKE LOWER(CONCAT('%', :key, '%')) 
                            OR LOWER(os.version) LIKE LOWER(CONCAT('%', :key, '%'))
                        )
                        AND (:status IS NULL OR os.status = :status)
                    """
    )
    Page<ADOperatingResponse> getAllOperatingSystems(
            Pageable pageable,
            @Param("key") String key,
            @Param("status") EntityStatus status
    );
}