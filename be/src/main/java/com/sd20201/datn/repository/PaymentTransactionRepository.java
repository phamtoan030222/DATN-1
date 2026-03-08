package com.sd20201.datn.repository;

import com.sd20201.datn.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByInvoiceCode(String invoiceCode);

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentTransaction t SET t.status = :status, t.transactionNo = :transactionNo, t.bankCode = :bankCode, t.updatedAt = CURRENT_TIMESTAMP WHERE t.invoiceCode = :invoiceCode")
    int updateByInvoiceCode(@Param("invoiceCode") String invoiceCode,
                            @Param("status") String status,
                            @Param("transactionNo") String transactionNo,
                            @Param("bankCode") String bankCode);
}