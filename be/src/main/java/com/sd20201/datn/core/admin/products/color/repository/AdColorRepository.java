package com.sd20201.datn.core.admin.products.color.repository;

import com.sd20201.datn.core.admin.products.color.model.request.AdColorRequest;
import com.sd20201.datn.core.admin.products.color.model.response.AdColorResponse;
import com.sd20201.datn.entity.Color;
import com.sd20201.datn.repository.ColorRepository;
import jakarta.validation.constraints.NotBlank;
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
           WHERE (:colorName IS NULL OR c.name LIKE CONCAT('%', :colorName, '%'))
                 AND (:colorStatus IS NULL OR c.status = :colorStatus)
           ORDER BY c.createdDate DESC
    """,
            countQuery = """
           SELECT COUNT(c.id)
           FROM Color c 
           WHERE (:colorName IS NULL OR c.name LIKE CONCAT('%', :colorName, '%'))
                 AND (:colorStatus IS NULL OR c.status = :colorStatus)
    """
    )
    Page<AdColorResponse> getAllColors(Pageable pageable,
                                       @Param("colorName") String colorName,
                                       @Param("colorStatus") String colorStatus);


    List<Color> findAllByName(String name);

    List<Color> findAllByCode(String code);
}
