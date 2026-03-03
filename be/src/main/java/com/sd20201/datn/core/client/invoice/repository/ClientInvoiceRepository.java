package com.sd20201.datn.core.client.invoice.repository;

import com.sd20201.datn.core.client.invoice.model.response.ClientInvoiceDetailResponse;
import com.sd20201.datn.repository.InvoiceRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClientInvoiceRepository extends InvoiceRepository {

    @Query(value = """
    SELECT
        i.id as id
        , i.code as code
        , i.entityTrangThaiHoaDon as invoiceStatus
        , i.totalAmountAfterDecrease as totalAmountAfterDecrease
        , COUNT(imei.id) as totalQuantity
    FROM Invoice i
    LEFT JOIN InvoiceDetail ivd on ivd.invoice.id = i.id
    LEFT JOIN IMEI imei on imei.invoiceDetail.id = ivd.id
    WHERE i.code = :code AND i.status = 0 AND i.customer.id = :customerId AND i.typeInvoice = 1
    """)
    Optional<ClientInvoiceDetailResponse> getInvoiceByCode(String code, String customerId);
}
