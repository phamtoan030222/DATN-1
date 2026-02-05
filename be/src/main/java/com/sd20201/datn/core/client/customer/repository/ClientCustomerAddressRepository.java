package com.sd20201.datn.core.client.customer.repository;

import com.sd20201.datn.core.client.customer.model.response.ClientCustomerAddressResponse;
import com.sd20201.datn.repository.AddressRepository;
import org.springframework.data.jpa.repository.Query;

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

}
