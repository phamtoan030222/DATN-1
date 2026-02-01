package com.sd20201.datn.core.admin.banhang.repository;

import com.sd20201.datn.core.admin.banhang.model.response.ListHoaDon;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.repository.InvoiceRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ADTaoHoaDonRepository extends InvoiceRepository {

    @Query(value = """
    SELECT hd.id as id,
            hd.code as code,
            sum(hdct.quantity) as soLuong,
            hd.type_invoice as loaiHoaDon
    FROM invoice hd
    LEFT JOIN invoice_detail hdct on hd.id = hdct.id_invoice
    WHERE hd.trang_thai_hoa_don = 0 and (hd.type_invoice = 0 or hd.type_invoice = 2)
    GROUP BY hd.id, hd.code
    ORDER BY hd.created_date ASC
    """, nativeQuery = true)
    List<ListHoaDon> getAll();

    @Query("""
    SELECT DISTINCT pgg
    FROM Voucher pgg
    LEFT JOIN VoucherDetail  pggct ON pgg.id = pggct.voucher.id
    WHERE pgg.status = 0
    AND pgg.conditions <= :tong
    AND pgg.quantity > 0
    AND (
        pgg.targetType = 0
        OR (pgg.targetType = 1 AND pggct.customer.id = :id)
        )
    AND NOT EXISTS (
        SELECT 1
        FROM Invoice  hd
        WHERE hd.voucher.id = pgg.id
        AND hd.customer.id = :id
        )
    """)
    List<Voucher> getPhieuGiamGia(@Param("id") String id, @Param("tong")BigDecimal tong);

    // Native query đơn giản - sử dụng số cho enum
//    @Query(value = """
//        SELECT DISTINCT v.* FROM voucher v
//        LEFT JOIN voucher_detail vd ON v.id = vd.id_voucher
//        WHERE (v.target_type = 1  -- 1 = ALL_CUSTOMERS
//               OR (v.target_type = 0  -- 0 = INDIVIDUAL
//                   AND vd.id_customer = :customerId
//                   AND (vd.usage_status IS NULL OR vd.usage_status = 0)))
//        AND (v.start_date IS NULL OR v.start_date <= :currentTime)
//        AND (v.end_date IS NULL OR v.end_date >= :currentTime)
//        AND (v.remaining_quantity IS NULL OR v.remaining_quantity > 0)
//        AND (v.conditions IS NULL OR v.conditions <= :totalAmount)
//        ORDER BY v.created_date DESC
//    """, nativeQuery = true)
//    List<Voucher> getAvailableVouchersNative(
//            @Param("customerId") String customerId,
//            @Param("totalAmount") BigDecimal totalAmount,
//            @Param("currentTime") Long currentTime
//    );
//
//    @Query(value = """
//        SELECT DISTINCT v.* FROM voucher v
//        LEFT JOIN voucher_detail vd ON v.id = vd.id_voucher
//        WHERE (v.target_type = 1  -- 1 = ALL_CUSTOMERS
//               OR (v.target_type = 0  -- 0 = INDIVIDUAL
//                   AND vd.id_customer = :customerId
//                   AND (vd.usage_status IS NULL OR vd.usage_status = 0)))
//        AND (v.start_date IS NULL OR v.start_date <= :currentTime)
//        AND (v.end_date IS NULL OR v.end_date >= :currentTime)
//        AND (v.remaining_quantity IS NULL OR v.remaining_quantity > 0)
//        ORDER BY v.created_date DESC
//    """, nativeQuery = true)
//    List<Voucher> getAllVouchersNative(
//            @Param("customerId") String customerId,
//            @Param("currentTime") Long currentTime
//    );

      // Lấy voucher khả dụng cho khách hàng với điều kiện tổng tiền
        @Query("""
        SELECT v 
        FROM Voucher v
        LEFT JOIN v.voucherCustomers vc
        WHERE (
            v.targetType = com.sd20201.datn.infrastructure.constant.TargetType.ALL_CUSTOMERS 
            OR (
                v.targetType = com.sd20201.datn.infrastructure.constant.TargetType.INDIVIDUAL 
                AND vc.customer.id = :customerId 
                AND (vc.usageStatus IS NULL OR vc.usageStatus = false)
            )
        )
        AND (v.startDate IS NULL OR v.startDate <= :currentTime)
        AND (v.endDate IS NULL OR v.endDate >= :currentTime)
        AND (v.remainingQuantity IS NULL OR v.remainingQuantity > 0)
        AND (v.conditions IS NULL OR v.conditions <= :totalAmount)
        ORDER BY v.createdDate DESC
    """)
        List<Voucher> getAvailableVouchers(
                @Param("customerId") String customerId,
                @Param("totalAmount") BigDecimal totalAmount,
                @Param("currentTime") Long currentTime
        );

        // Lấy tất cả voucher cho khách hàng (không kiểm tra điều kiện tổng tiền)
        @Query("""
        SELECT v 
        FROM Voucher v
        LEFT JOIN v.voucherCustomers vc
        WHERE (
            v.targetType = com.sd20201.datn.infrastructure.constant.TargetType.ALL_CUSTOMERS 
            OR (
                v.targetType = com.sd20201.datn.infrastructure.constant.TargetType.INDIVIDUAL 
                AND vc.customer.id = :customerId 
                AND (vc.usageStatus IS NULL OR vc.usageStatus = false)
            )
        )
        AND (v.startDate IS NULL OR v.startDate <= :currentTime)
        AND (v.endDate IS NULL OR v.endDate >= :currentTime)
        AND (v.remainingQuantity IS NULL OR v.remainingQuantity > 0)
        ORDER BY v.createdDate DESC
    """)
        List<Voucher> getAllVouchers(
                @Param("customerId") String customerId,
                @Param("currentTime") Long currentTime
        );

        // Native Query fallback nếu JPQL không hoạt động
        @Query(value = """
        SELECT v.* 
        FROM voucher v
        LEFT JOIN voucher_detail vd ON v.id = vd.id_voucher 
        WHERE (
            v.target_type = 1 
            OR (
                v.target_type = 0 
                AND vd.id_customer = :customerId 
                AND (vd.usage_status IS NULL OR vd.usage_status = 0)
            )
        )
        AND (v.start_date IS NULL OR v.start_date <= :currentTime)
        AND (v.end_date IS NULL OR v.end_date >= :currentTime)
        AND (v.remaining_quantity IS NULL OR v.remaining_quantity > 0)
        AND (v.conditions IS NULL OR v.conditions <= :totalAmount)
        ORDER BY v.created_date DESC
    """, nativeQuery = true)
        List<Voucher> getAvailableVouchersNative(
                @Param("customerId") String customerId,
                @Param("totalAmount") BigDecimal totalAmount,
                @Param("currentTime") Long currentTime
        );

        @Query(value = """
        SELECT v.* 
        FROM voucher v
        LEFT JOIN voucher_detail vd ON v.id = vd.id_voucher 
        WHERE (
            v.target_type = 1 
            OR (
                v.target_type = 0 
                AND vd.id_customer = :customerId 
                AND (vd.usage_status IS NULL OR vd.usage_status = 0)
            )
        )
        AND (v.start_date IS NULL OR v.start_date <= :currentTime)
        AND (v.end_date IS NULL OR v.end_date >= :currentTime)
        AND (v.remaining_quantity IS NULL OR v.remaining_quantity > 0)
        ORDER BY v.created_date DESC
    """, nativeQuery = true)
        List<Voucher> getAllVouchersNative(
                @Param("customerId") String customerId,
                @Param("currentTime") Long currentTime
        );

}

