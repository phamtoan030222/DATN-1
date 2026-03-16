package com.sd20201.datn.repository;

import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.entity.VoucherDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherDetailRepository extends JpaRepository<VoucherDetail, String> {
    boolean existsByVoucherAndCustomer(Voucher voucher, Customer customer);

    void deleteByVoucher(Voucher voucher);

    VoucherDetail findByInvoiceId(String id);


    @Query("""
    SELECT vd FROM VoucherDetail vd WHERE vd.customer.id = :idCustomer AND vd.status = 0 AND vd.voucher.status = 0 AND vd.voucher.id = :idVoucher
    """)
    Optional<VoucherDetail> findByIdVoucherAndIdCustomer(String idVoucher, String idCustomer);
}
