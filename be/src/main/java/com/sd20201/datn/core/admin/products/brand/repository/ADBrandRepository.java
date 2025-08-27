package com.sd20201.datn.core.admin.products.brand.repository;

import com.sd20201.datn.core.admin.products.brand.model.response.ADBrandResponse;
import com.sd20201.datn.repository.BrandRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ADBrandRepository extends BrandRepository {
    @Query(
            value = """
        SELECT b.id AS id,
               b.name AS name,
               b.code AS code,
               b.status AS status
        FROM Brand b
        WHERE (:key IS NULL OR b.name LIKE CONCAT('%', :key, '%'))
        ORDER BY b.createdDate DESC
    """,
            countQuery = """
        SELECT COUNT(b.id)
        FROM Brand b
        WHERE (:key IS NULL OR b.name LIKE CONCAT('%', :key, '%'))
    """
    )
    Page<ADBrandResponse> getAllBrands(
            Pageable pageable,
            @Param("key") String key
    );
}
