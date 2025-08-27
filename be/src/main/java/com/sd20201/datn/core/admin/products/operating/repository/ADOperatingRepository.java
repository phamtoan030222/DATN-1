package com.sd20201.datn.core.admin.products.operating.repository;

import com.sd20201.datn.core.admin.products.operating.model.response.ADOperatingResponse;
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
               os.version AS version,
               os.description AS description,
               os.status AS status
        FROM OperatingSystem os
        WHERE (:key IS NULL OR os.name LIKE CONCAT('%', :key, '%') OR os.version LIKE CONCAT('%', :key, '%'))
        ORDER BY os.createdDate DESC
    """,
            countQuery = """
        SELECT COUNT(os.id)
        FROM OperatingSystem os
        WHERE (:key IS NULL OR os.name LIKE CONCAT('%', :key, '%') OR os.version LIKE CONCAT('%', :key, '%'))
    """
    )
    Page<ADOperatingResponse> getAllOperatingSystems(
            Pageable pageable,
            @Param("key") String key
    );
}
