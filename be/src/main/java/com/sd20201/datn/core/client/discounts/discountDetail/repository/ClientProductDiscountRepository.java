package com.sd20201.datn.core.client.discounts.discountDetail.repository;

import com.sd20201.datn.core.client.discounts.discountDetail.model.respone.ClientProductRespone;
import com.sd20201.datn.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;


public interface ClientProductDiscountRepository extends ProductRepository {
    @Query(value = """
            SELECT c.id AS id,
                   c.code AS productCode,
                   c.name AS productName,
                   b.name AS productBrand,
                   c.createdDate AS createdDate,
                   COUNT(pd.id) AS quantity
            FROM Product c
                     JOIN c.brand b
                     LEFT JOIN ProductDetail pd ON pd.product = c
            WHERE c.status = 0
            GROUP BY c.id, c.code, c.name, b.name
            ORDER BY c.createdDate ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c.id)
            FROM Product c
                     JOIN c.brand b
                     LEFT JOIN ProductDetail pd ON pd.product = c
            WHERE c.status = 0
            """)
    Page<ClientProductRespone> getAllProducts(Pageable pageable);

}
