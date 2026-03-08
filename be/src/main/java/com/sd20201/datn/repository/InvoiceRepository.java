package com.sd20201.datn.repository;

import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.infrastructure.constant.TrangThaiThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    // SỬA LẠI - dùng field 'code' từ PrimaryEntity
    Optional<Invoice> findByCode(String code);

    boolean existsByCode(String code);

    @Modifying
    @Transactional
    @Query("UPDATE Invoice i SET i.trangThaiThanhToan = :trangThai, " +
            "i.paymentDate = :paymentDate WHERE i.code = :code")
    int updateTrangThaiThanhToanByCode(@Param("code") String code,
                                       @Param("trangThai") TrangThaiThanhToan trangThai,
                                       @Param("paymentDate") Long paymentDate);
}