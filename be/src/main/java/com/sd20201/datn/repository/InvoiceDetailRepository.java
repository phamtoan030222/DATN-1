package com.sd20201.datn.repository;

import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.entity.InvoiceDetail;
import com.sd20201.datn.entity.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, String> {
    InvoiceDetail findByInvoiceAndProductDetail(Invoice invoice, ProductDetail productDetail);

    Optional<InvoiceDetail> findByInvoiceIdAndProductDetailId(String invoiceId, String productDetailId);

    @Query("""
       SELECT COALESCE(SUM(i.totalAmount), 0)
       FROM InvoiceDetail i
       WHERE i.invoice.id = :invoiceId
       """)
    BigDecimal sumTotalAmountByInvoiceId(@Param("invoiceId") String invoiceId);
}
