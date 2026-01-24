package com.sd20201.datn.core.admin.banhang.repository;

import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.entity.VoucherDetail;
import com.sd20201.datn.infrastructure.constant.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ADBHVoucherDetailRepository extends JpaRepository<VoucherDetail, String> {

    // Tìm bản ghi phân phối voucher cho một khách hàng cụ thể
    Optional<VoucherDetail> findByVoucherAndCustomerId(Voucher voucher, String customerId);

    // Tìm tất cả voucher của một khách hàng theo trạng thái sử dụng
    List<VoucherDetail> findByCustomerIdAndUsageStatus(String customerId, Boolean usageStatus);

    // Kiểm tra xem một khách hàng đã sử dụng một voucher chưa
    boolean existsByVoucherAndCustomerIdAndUsageStatus(Voucher voucher, String customerId, Boolean usageStatus);

    /**
     * Tìm tất cả voucher còn sử dụng được của một khách hàng
     */
    @Query("SELECT vd FROM VoucherDetail vd WHERE " +
            "vd.customer.id = :customerId AND " +             // Của khách hàng này
            "(vd.usageStatus IS NULL OR vd.usageStatus = false) AND " + // Chưa sử dụng
            "vd.voucher.status = 0 AND " +                 // Voucher đang hoạt động
            "vd.voucher.startDate <= :currentTime AND " +     // Đã bắt đầu
            "vd.voucher.endDate >= :currentTime AND " +       // Chưa kết thúc
            "vd.voucher.remainingQuantity > 0")               // Còn số lượng
    List<VoucherDetail> findAvailableVouchersForCustomer(
            @Param("customerId") String customerId,
            @Param("currentTime") Long currentTime);

    /**
     * Tìm voucher sắp hết hạn của một khách hàng (chưa thông báo)
     */
    @Query("SELECT vd FROM VoucherDetail vd WHERE " +
            "vd.customer.id = :customerId AND " +
            "(vd.usageStatus IS NULL OR vd.usageStatus = false) AND " + // Chưa sử dụng
            "vd.expiredNotified = false AND " +                        // Chưa thông báo
            "vd.voucher.status = 0 AND " +
            "vd.voucher.endDate <= :expireTime AND " +                 // Sắp hết hạn
            "vd.voucher.endDate >= :currentTime")                      // Chưa hết hạn
    List<VoucherDetail> findExpiringVouchersForCustomer(
            @Param("customerId") String customerId,
            @Param("currentTime") Long currentTime,
            @Param("expireTime") Long expireTime);

    /**
     * Đếm số lượng voucher đã sử dụng của một khách hàng
     */
    @Query("SELECT COUNT(vd) FROM VoucherDetail vd WHERE " +
            "vd.customer.id = :customerId AND " +
            "vd.usageStatus = true")
    Long countUsedVouchersByCustomer(@Param("customerId") String customerId);

    @Query("""
        SELECT v FROM Voucher v
        WHERE v.targetType = :target
          AND v.remainingQuantity > 0
          AND v.startDate <= :now
          AND v.endDate >= :now
    """)
    List<Voucher> findVoucherHopLe(
            @Param("target") TargetType target,
            @Param("now") Long now
    );

    @Query("""
        SELECT vd.voucher FROM VoucherDetail vd
        WHERE vd.customer.id = :customerId
          AND (vd.usageStatus IS NULL OR vd.usageStatus = false)
    """)
    List<Voucher> findVoucherRiengCuaKH(
            @Param("customerId") String customerId
    );
}