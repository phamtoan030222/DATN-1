package com.sd20201.datn.core.client.customer.repository;

import com.sd20201.datn.core.admin.customer.model.response.AddressResponse;
import com.sd20201.datn.core.client.customer.model.response.ClientCustomerAddressResponse;
import com.sd20201.datn.entity.Address;
import com.sd20201.datn.repository.AddressRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientCustomerAddressRepository extends AddressRepository {

    @Query(value = """
    SELECT
        a.id as id,
        a.addressDetail as detail,
        a.wardCommune as wardCommune,
        a.provinceCity as province,
        a.isDefault as isDefault
    FROM Address a
    WHERE a.customer.id = :customerId AND a.isDefault = TRUE
    """)
    List<ClientCustomerAddressResponse> getAddressByCustomerId(String customerId);

    @Query(value = """
    SELECT
        a.id as id,
        a.addressDetail as detail,
        a.wardCommune as wardCommune,
        a.provinceCity as province,
        a.isDefault as isDefault
    FROM Address a
    WHERE a.id = :id and a.status = 0
    """)
    Optional<ClientCustomerAddressResponse> findAddressById(String id);

    @Query(
            value = """
            SELECT a.id AS id,
                   a.provinceCity AS provinceCity,
                   a.wardCommune AS wardCommune,
                   a.addressDetail AS addressDetail,
                   a.isDefault AS isDefault,
                   a.customer.id AS customerId,
                   a.createdDate AS createdDate,
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
