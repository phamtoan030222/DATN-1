package com.sd20201.datn.core.admin.invoices.invoice.repository;

import com.sd20201.datn.repository.InvoiceRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdInvoiceRepository extends InvoiceRepository {
    @Query("""
            SELECT i.id AS id
                   i.
            FROM Invoice i 
            """)
}
