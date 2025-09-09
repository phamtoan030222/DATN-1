package com.sd20201.datn.core.admin.customer.repository;

import com.sd20201.datn.core.admin.customer.model.response.AddressResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.sd20201.datn.entity.Address;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdAddressRepository extends com.sd20201.datn.repository.AddressRepository{
    @Query(
            value = """
            SELECT a.id AS id,
                   a.provinceCity AS provinceCity,
                   a.wardCommune AS wardCommune,
                   a.addressDetail AS addressDetail,
                   a.isDefault AS isDefault,
                   a.customer.id AS customerId,
                   a.createdBy AS createdBy,
                   a.createdDate AS createdDate,
                   a.lastModifiedBy AS modifiedBy,
                   a.lastModifiedDate AS modifiedDate
            FROM Address a
            WHERE (:customerId IS NULL OR a.customer.id = :customerId)
              AND (:provinceCity IS NULL OR a.provinceCity LIKE CONCAT('%', :provinceCity, '%'))
            ORDER BY a.createdDate DESC
            """,
            countQuery = """
            SELECT COUNT(a.id)
            FROM Address a
            WHERE (:customerId IS NULL OR a.customer.id = :customerId)
              AND (:provinceCity IS NULL OR a.provinceCity LIKE CONCAT('%', :provinceCity, '%'))
            """
    )
    Page<AddressResponse> getAllAddresses(Pageable pageable,
                                          @Param("customerId") String customerId,
                                          @Param("provinceCity") String provinceCity);

    // =========================
    // 🎯 Query đặc thù
    // =========================
    List<Address> findByCustomerId(String customerId);

    Optional<Address> findByCustomerIdAndIsDefaultTrue(String customerId);
}
