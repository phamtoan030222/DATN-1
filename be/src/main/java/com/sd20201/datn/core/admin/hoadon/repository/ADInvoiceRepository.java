package com.sd20201.datn.core.admin.hoadon.repository;

import com.sd20201.datn.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ADInvoiceRepository extends JpaRepository<Invoice, String>, ADHoaDonRepositoryCustom {

    @Query("SELECT h FROM Invoice h WHERE h.code = :ma")
    Optional<Invoice> findByMa(@Param("ma") String ma);
}