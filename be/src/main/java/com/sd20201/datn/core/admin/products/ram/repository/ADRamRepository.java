package com.sd20201.datn.core.admin.products.ram.repository;

import com.sd20201.datn.core.admin.products.ram.model.response.AdRamResponse;
import com.sd20201.datn.repository.RAMRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

@Repository
public interface ADRamRepository extends RAMRepository {

    @Query(
            value = """
           SELECT r.id AS id,
                  r.name AS name,
                  r.status AS status,
                  r.brand AS brand,
                  r.capacity AS capacity,
                  r.type AS type,
                  r.busSpeed AS busSpeed,
                  r.maxSupported AS maxSupported,
                  r.slotConFig AS slotConFig,
                  r.description AS description
           FROM RAM r
           WHERE (:name IS NULL OR r.name LIKE CONCAT('%', :name, '%')
               OR r.brand LIKE CONCAT('%', :name, '%'))
           ORDER BY r.createdDate DESC
    """, countQuery = """
           SELECT COUNT(r.id)
           FROM RAM r
           WHERE :name IS NULL OR r.name LIKE CONCAT('%', :name, '%')
               OR r.brand LIKE CONCAT('%', :name, '%')
    """
    )
    Page<AdRamResponse> getAllRam(Pageable pageable,
                                  @Param("name") String name);

}
