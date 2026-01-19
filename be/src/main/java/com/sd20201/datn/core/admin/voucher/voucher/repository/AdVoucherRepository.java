package com.sd20201.datn.core.admin.voucher.voucher.repository;

import com.sd20201.datn.core.admin.voucher.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.admin.voucher.voucher.model.response.AdCustomerResponse;
import com.sd20201.datn.core.admin.voucher.voucher.model.response.AdVoucherResponse;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.repository.VoucherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdVoucherRepository extends VoucherRepository {

    @Query(
            value = """
                        SELECT
                          v.id as id, v.code as code, v.name as name,
                          v.typeVoucher as typeVoucher, v.discountValue as discountValue,
                          v.targetType as targetType, v.quantity as quantity,
                          v.remainingQuantity as remainingQuantity, v.maxValue as maxValue,
                          v.startDate as startDate, v.endDate as endDate,
                          v.conditions as conditions, v.status as status,
                          v.createdDate as createdDate,
                          v.note as note
                        FROM Voucher v
                        WHERE
                          ( :#{#request.q} IS NULL
                            OR v.name LIKE CONCAT('%', :#{#request.q}, '%')
                            OR v.code LIKE CONCAT('%', :#{#request.q}, '%')
                          )
                          AND ( :#{#request.startDate}   IS NULL OR v.startDate >= :#{#request.startDate} )
                          AND ( :#{#request.endDate}     IS NULL OR v.endDate   <= :#{#request.endDate} )
                          AND ( :#{#request.typeVoucher} IS NULL OR v.typeVoucher = :#{#request.typeVoucher} )
                          AND ( :#{#request.targetType}  IS NULL OR v.targetType  = :#{#request.targetType} )
                          AND ( :#{#request.status}      IS NULL OR v.status      = :#{#request.status} )
                          AND (
                              :#{#request.period} IS NULL 
                              OR (:#{#request.period} = 'UPCOMING' AND v.startDate > :#{#request.now})
                              OR (:#{#request.period} = 'ENDED'    AND (v.endDate < :#{#request.now} OR v.remainingQuantity <= 0))
                              OR (:#{#request.period} = 'ONGOING'  
                                  AND v.startDate <= :#{#request.now} 
                                  AND v.endDate >= :#{#request.now} 
                                  AND v.remainingQuantity > 0
                                  AND v.status = 0 
                              )
                              OR (:#{#request.period} = 'PAUSED'   
                                  AND v.startDate <= :#{#request.now} 
                                  AND v.endDate >= :#{#request.now} 
                                  AND v.remainingQuantity > 0
                                  AND v.status = 1 
                              )
                          )
                        ORDER BY v.createdDate DESC
                    """,
            countQuery = """
        SELECT COUNT(v)
        FROM Voucher v
        WHERE
          ( :#{#request.q} IS NULL OR v.name LIKE CONCAT('%', :#{#request.q}, '%') OR v.code LIKE CONCAT('%', :#{#request.q}, '%') )
          AND ( :#{#request.startDate}   IS NULL OR v.startDate >= :#{#request.startDate} )
          AND ( :#{#request.endDate}     IS NULL OR v.endDate   <= :#{#request.endDate} )
          AND ( :#{#request.typeVoucher} IS NULL OR v.typeVoucher = :#{#request.typeVoucher} )
          AND ( :#{#request.targetType}  IS NULL OR v.targetType  = :#{#request.targetType} )
          AND ( :#{#request.status}      IS NULL OR v.status      = :#{#request.status} )
          AND (
              :#{#request.period} IS NULL 
              OR (:#{#request.period} = 'UPCOMING' AND v.startDate > :#{#request.now})
              OR (:#{#request.period} = 'ENDED'    AND (v.endDate < :#{#request.now} OR v.remainingQuantity <= 0))
              OR (:#{#request.period} = 'ONGOING'  AND v.startDate <= :#{#request.now} AND v.endDate >= :#{#request.now} AND v.remainingQuantity > 0 AND v.status = 0)
              OR (:#{#request.period} = 'PAUSED'   AND v.startDate <= :#{#request.now} AND v.endDate >= :#{#request.now} AND v.remainingQuantity > 0 AND v.status = 1)
          )
    """
    )
    Page<AdVoucherResponse> getVouchers(Pageable pageable, @Param("request") AdVoucherRequest request);

    @Query("""
                SELECT
                  v.id as id, v.code as code, v.name as name, v.typeVoucher as typeVoucher,
                  v.discountValue as discountValue, v.targetType as targetType, v.quantity as quantity,
                  v.remainingQuantity as remainingQuantity, v.maxValue as maxValue,
                  v.startDate as startDate, v.endDate as endDate, v.conditions as conditions,
                  v.status as status, v.createdDate as createdDate, v.note as note
                FROM Voucher v WHERE v.id = :id
            """)
    Optional<AdVoucherResponse> getVoucherById(@Param("id") String id);


    // Dùng đúng tên mà service của bạn gọi
    Optional<Voucher> findVoucherByName(String name);

    /* ===================== Voucher <-> Customer ===================== */
    @Query("""
                SELECT v, vd.customer
                FROM Voucher v
                LEFT JOIN v.voucherCustomers vd
                WHERE (:onlyUsed = false OR vd.usageStatus = true)
            """)
    List<Object[]> getVouchersWithCustomers(@Param("onlyUsed") boolean onlyUsed);

    // Tìm khách hàng theo voucher
    @Query(
            value = """
                        SELECT
                            c.id as id,
                            c.code as customerCode,
                            c.name as customerName,
                            c.phone as customerPhone,
                            c.email as customerEmail,
                            c.status as customerStatus
                        FROM VoucherDetail vd
                        JOIN vd.customer c
                        WHERE vd.voucher.id = :voucherId
                    """,
            countQuery = """
                        SELECT count(c)
                        FROM VoucherDetail vd
                        JOIN vd.customer c
                        WHERE vd.voucher.id = :voucherId
                    """
    )
    Page<AdCustomerResponse> findAssignedCustomersByVoucherId(@Param("voucherId") String voucherId, Pageable pageable);

    @Query(
            value = """
                        SELECT
                            c.id as id,
                            c.code as customerCode,
                            c.name as customerName,
                            c.phone as customerPhone,
                            c.email as customerEmail,
                            c.status as customerStatus
                        FROM VoucherDetail vd
                        JOIN vd.customer c
                        WHERE vd.voucher.code = :code
                          AND vd.usageStatus = true
                    """,
            countQuery = """
                        SELECT count(c)
                        FROM VoucherDetail vd
                        JOIN vd.customer c
                        WHERE vd.voucher.code = :code
                          AND vd.usageStatus = true
                    """
    )
    Page<AdCustomerResponse> findUsedCustomersByVoucherCode(@Param("code") String code, Pageable pageable);

    @Query("""
    SELECT DISTINCT p FROM Voucher p
    LEFT JOIN VoucherDetail pggct ON p.id = pggct.voucher.id
    WHERE p.status = 0
    AND p.quantity > 0
    AND (
        p.targetType = 0
        OR (p.targetType = 1 AND pggct.customer.id = :id)  
        )
    AND NOT EXISTS (
        SELECT 1 FROM Invoice  hd
        WHERE hd.voucher.id = p.id
        AND hd.customer.id = : id
        AND hd.entityTrangThaiHoaDon = 4 
        )
    """)
    List<Voucher> findAvailableVouchers(@Param("id") String id);
}

