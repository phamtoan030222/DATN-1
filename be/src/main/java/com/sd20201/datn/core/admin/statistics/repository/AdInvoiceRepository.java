package com.sd20201.datn.core.admin.statistics.repository;

import com.sd20201.datn.core.admin.statistics.model.response.AdInvoiceResponse;
import com.sd20201.datn.repository.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AdInvoiceRepository extends InvoiceRepository {
    @Query(value = """
            SELECT b.id AS id,
                   d.name AS productName,
                   e.name AS productBrand,
                   a.quantity AS quantity,
                   a.price AS price,
                   b.createdDate AS createdDate
            FROM InvoiceDetail a
                     JOIN a.invoice b
                     JOIN a.productDetail c
                     JOIN c.product d
                     JOIN d.brand e
            WHERE a.status = 0 AND b.status = 0
            ORDER BY createdDate DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT b.id)
            FROM InvoiceDetail a
                     JOIN a.invoice b
                     JOIN a.productDetail c
                     JOIN c.product d
            WHERE a.status = 0 AND b.status = 0
            """)
    Page<AdInvoiceResponse> getAllInvoice(Pageable pageable);

}
