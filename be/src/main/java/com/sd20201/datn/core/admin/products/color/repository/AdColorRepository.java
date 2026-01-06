package com.sd20201.datn.core.admin.products.color.repository;

import com.sd20201.datn.core.admin.products.color.model.response.AdColorResponse;
import com.sd20201.datn.entity.Color;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.repository.ColorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdColorRepository extends ColorRepository {

    @Query(
            value = """
           SELECT c.id AS id,
                  c.name AS colorName,
                  c.code AS colorCode,
                  c.createdDate AS createdDate,
                  c.status AS colorStatus
           FROM Color c 
           WHERE (
                 :keyword IS NULL 
                 OR c.name LIKE CONCAT('%', :keyword, '%') 
                 OR c.code LIKE CONCAT('%', :keyword, '%')
           )
           AND (:status IS NULL OR c.status = :status)
           ORDER BY c.createdDate DESC
    """,
            countQuery = """
           SELECT COUNT(c.id)
           FROM Color c 
           WHERE (
                 :keyword IS NULL 
                 OR c.name LIKE CONCAT('%', :keyword, '%') 
                 OR c.code LIKE CONCAT('%', :keyword, '%')
           )
           AND (:status IS NULL OR c.status = :status)
    """
    )
    Page<AdColorResponse> getAllColors(Pageable pageable,
                                       @Param("keyword") String keyword, // Tìm chung cho cả Name và Code
                                       @Param("status") EntityStatus status);


    List<Color> findAllByName(String name);

    List<Color> findAllByCode(String code);
}