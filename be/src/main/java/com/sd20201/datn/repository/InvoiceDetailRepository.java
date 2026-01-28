package com.sd20201.datn.repository;

import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.entity.InvoiceDetail;
import com.sd20201.datn.entity.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, String> {
    InvoiceDetail findByInvoiceAndProductDetail(Invoice invoice, ProductDetail productDetail);
}
