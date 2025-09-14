package com.sd20201.datn.core.admin.discount.voucher.repository;

import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.admin.discount.voucher.model.response.AdVoucherResponse;
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

    /* ===================== VOUCHER LIST (projection) ===================== */
    @Query(
            value = """
            SELECT
              v.id as id,
              v.code as code,
              v.name as name,
              v.typeVoucher as typeVoucher,
              v.discountValue as discountValue,
              v.targetType as targetType,
              v.quantity as quantity,
              v.remainingQuantity as remainingQuantity,
              v.maxValue as maxValue,
              v.startDate as startDate,
              v.endDate as endDate,
              v.conditions as conditions,
              v.status as status,
              v.createdDate as createdDate
            FROM Voucher v
            WHERE
              ( :#{#request.q} IS NULL
                OR v.name LIKE CONCAT('%', :#{#request.q}, '%')
                OR v.code LIKE CONCAT('%', :#{#request.q}, '%')
              )
              AND ( :#{#request.startDate} IS NULL OR v.startDate >= :#{#request.startDate} )
              AND ( :#{#request.endDate}   IS NULL OR v.endDate   <= :#{#request.endDate} )
              AND ( :#{#request.conditions} IS NULL OR v.conditions <= :#{#request.conditions})
              AND ( :#{#request.status} IS NULL OR :#{#request.status} = v.status )
            ORDER BY v.createdDate DESC
        """,
            countQuery = """
            SELECT COUNT(v)
            FROM Voucher v
            WHERE
              ( :#{#request.q} IS NULL
                OR v.name LIKE CONCAT('%', :#{#request.q}, '%')
                OR v.code LIKE CONCAT('%', :#{#request.q}, '%')
              )
              AND ( :#{#request.startDate} IS NULL OR v.startDate >= :#{#request.startDate} )
              AND ( :#{#request.endDate}   IS NULL OR v.endDate   <= :#{#request.endDate} )
              AND ( :#{#request.conditions} IS NULL OR v.conditions <= :#{#request.conditions})
              AND ( :#{#request.status} IS NULL OR :#{#request.status} = v.status )
        """
    )
    Page<AdVoucherResponse> getVouchers(Pageable pageable, @Param("request") AdVoucherRequest request);

    @Query("""
        SELECT
          v.id as id, v.code as code, v.name as name, v.typeVoucher as typeVoucher,
          v.discountValue as discountValue, v.targetType as targetType, v.quantity as quantity,
          v.remainingQuantity as remainingQuantity, v.maxValue as maxValue,
          v.startDate as startDate, v.endDate as endDate, v.conditions as conditions,
          v.status as status, v.createdDate as createdDate
        FROM Voucher v WHERE v.id = :id
    """)
    Optional<AdVoucherResponse> getVoucherById(@Param("id") String id);

    /* ===================== Finders ===================== */
    Optional<Voucher> findByCode(String code);

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

    /* --------- KH được gán vào voucher (theo voucherId) --------- */
    @Query(
            value = """
            select c
            from VoucherDetail vd
            join vd.customer c
            where vd.voucher.id = :voucherId
        """,
            countQuery = """
            select count(c)
            from VoucherDetail vd
            join vd.customer c
            where vd.voucher.id = :voucherId
        """
    )
    Page<Customer> findAssignedCustomersByVoucherId(@Param("voucherId") String voucherId, Pageable pageable);

    /* --------- KH đã sử dụng voucher (theo voucherCode) --------- */
    @Query(
            value = """
            select c
            from VoucherDetail vd
            join vd.customer c
            where vd.voucher.code = :code
              and vd.usageStatus = true
        """,
            countQuery = """
            select count(c)
            from VoucherDetail vd
            join vd.customer c
            where vd.voucher.code = :code
              and vd.usageStatus = true
        """
    )
    Page<Customer> findUsedCustomersByVoucherCode(@Param("code") String code, Pageable pageable);

    /* (Tuỳ chọn) Non-paged tiện dùng nhanh nếu cần */
    @Query("""
        select c
        from VoucherDetail vd
        join vd.customer c
        where vd.voucher.id = :voucherId
    """)
    List<Customer> findAssignedCustomersByVoucherIdRaw(@Param("voucherId") String voucherId);

    @Query("""
        select c
        from VoucherDetail vd
        join vd.customer c
        where vd.voucher.code = :code
          and vd.usageStatus = true
    """)
    List<Customer> findUsedCustomersByVoucherCodeRaw(@Param("code") String code);
}
