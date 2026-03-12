package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDPropertyComboboxResponse;
import com.sd20201.datn.entity.RAM;
import com.sd20201.datn.repository.RAMRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientPDRAMRepository extends RAMRepository {

    @Query(value = """ 
    SELECT r.id as value, r.name as label FROM RAM r
    ORDER BY r.createdDate desc
    """)
    List<ClientPDPropertyComboboxResponse> getRAMs();

    @Query(value = """ 
        SELECT r FROM RAM r
        WHERE LOWER(r.name) = LOWER(:name)
    """)
    Optional<RAM> findByName(String name);

    @Query(value = """ 
                SELECT DISTINCT r.id as value, r.name as label, r.code as code
                FROM ProductDetail pd
                JOIN pd.ram r
                WHERE pd.product.id = (
                    SELECT pd2.product.id
                    FROM ProductDetail pd2
                    WHERE pd2.id = :pdId
                )
                ORDER BY r.name ASC
            """)
    List<ClientPDPropertyComboboxResponse> getCpuByPD(@Param("pdId") String pdId);

}
