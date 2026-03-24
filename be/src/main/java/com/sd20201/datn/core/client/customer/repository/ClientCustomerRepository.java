package com.sd20201.datn.core.client.customer.repository;

import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.repository.CustomerRepository;

import java.util.Optional;

public interface ClientCustomerRepository extends CustomerRepository {
    Optional<Customer> findByEmail(String email);
}
