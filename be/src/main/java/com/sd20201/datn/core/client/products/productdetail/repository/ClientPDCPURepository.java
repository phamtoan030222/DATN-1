package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDPropertyComboboxResponse;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.repository.CPURepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientPDCPURepository extends CPURepository {

    @Query(value = """ 
            SELECT c.id as value, c.name as label FROM CPU c
            ORDER BY c.createdDate desc
            """)
    List<ClientPDPropertyComboboxResponse> getCPUs();

    @Query(value = """ 
                SELECT c FROM CPU c
                WHERE LOWER(c.name) = LOWER(:name)
            """)
    Optional<CPU> findByName(String name);

    @Query(value = """
                SELECT * FROM cpu 
                WHERE product_detail_id = :productDetailId
            """, nativeQuery = true)
    List<CPU> findCPUsByProductDetailId(@Param("productDetailId") Long productDetailId);

    @Query(value = """ 
                SELECT DISTINCT c.id as value, c.name as label, c.code as code
                FROM ProductDetail pd
                JOIN pd.cpu c
                WHERE pd.product.id = (
                    SELECT pd2.product.id
                    FROM ProductDetail pd2
                    WHERE pd2.id = :pdId
                )
                ORDER BY c.name ASC
            """)
    List<ClientPDPropertyComboboxResponse> getCpuByPD(@Param("pdId") String pdId);

}
