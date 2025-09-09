package com.sd20201.datn.core.admin.discount.voucher.repository;

import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.admin.discount.voucher.model.response.AdVoucherResponse;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.repository.VoucherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdVoucherRepository extends VoucherRepository {

    @Query(value = """
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
        AND ( :#{#request.endDate} IS NULL OR v.endDate <= :#{#request.endDate} )
        AND ( :#{#request.conditions} IS NULL OR v.conditions <= :#{#request.conditions})
        AND (:#{#request.status} IS  NULL OR :#{#request.status} = v.status)
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
        AND ( :#{#request.endDate} IS NULL OR v.endDate <= :#{#request.endDate} )
        AND ( :#{#request.conditions} IS NULL OR v.conditions <= :#{#request.conditions})
        AND (:#{#request.status} IS  NULL OR :#{#request.status} = v.status)
    """)
    Page<AdVoucherResponse> getVouchers(Pageable pageable, AdVoucherRequest request);

    Optional<AdVoucherResponse> getVoucherById(@Param("id") String id);

    Optional<Voucher> findVoucherByCode(String code);
    Optional<Voucher> findVoucherByName(String name);
}
