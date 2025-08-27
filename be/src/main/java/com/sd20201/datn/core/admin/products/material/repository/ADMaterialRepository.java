package com.sd20201.datn.core.admin.products.material.repository;

import com.sd20201.datn.core.admin.products.material.model.response.ADMaterialResponse;
import com.sd20201.datn.repository.HardDriveRepository;
import com.sd20201.datn.repository.MaterialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ADMaterialRepository extends MaterialRepository {

    @Query(
            value = """
            SELECT m.id AS id,
                    m.code AS code,
                   m.topCaseMaterial AS topCaseMaterial,
                   m.bottomCaseMaterial AS bottomCaseMaterial,
                   m.keyboardMaterial AS keyboardMaterial,
                   m.status AS status
            FROM Material m
            WHERE (:key IS NULL OR m.topCaseMaterial LIKE CONCAT('%', :key, '%')
                OR m.bottomCaseMaterial LIKE CONCAT('%', :key, '%')
                OR m.keyboardMaterial LIKE CONCAT('%', :key, '%'))
            ORDER BY m.createdDate DESC
        """,
            countQuery = """
            SELECT COUNT(m.id)
            FROM Material m
            WHERE (:key IS NULL OR m.topCaseMaterial LIKE CONCAT('%', :key, '%')
                OR m.bottomCaseMaterial LIKE CONCAT('%', :key, '%')
                OR m.keyboardMaterial LIKE CONCAT('%', :key, '%'))
        """
    )
    Page<ADMaterialResponse> getAllMaterials(
            Pageable pageable,
            @Param("key") String key
    );
}
