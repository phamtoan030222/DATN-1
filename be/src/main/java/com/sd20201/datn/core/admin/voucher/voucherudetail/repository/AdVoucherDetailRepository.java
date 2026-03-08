package com.sd20201.datn.core.admin.voucher.voucherudetail.repository;

import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.entity.VoucherDetail;
import com.sd20201.datn.repository.VoucherDetailRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdVoucherDetailRepository extends VoucherDetailRepository {
    boolean existsByVoucherAndCustomer(Voucher voucher, Customer customer);

    void deleteByVoucher(Voucher voucher);

    long countByVoucherId(String voucherId);

    // [MỚI] Thêm hàm này để lấy danh sách detail theo voucher
    List<VoucherDetail> findAllByVoucher(Voucher voucher);

    @Query("SELECT vd FROM VoucherDetail vd WHERE vd.voucher.id = :voucherId AND vd.customer.id = :customerId")
    Optional<VoucherDetail> findByVoucherIdAndCustomerId(@Param("voucherId") String voucherId,
                                                         @Param("customerId") String customerId);
}
