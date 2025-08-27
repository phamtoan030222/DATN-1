package com.sd20201.datn.infrastructure.secutiry.repository;

import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.repository.CustomerRepository;

import java.util.Optional;

public interface AuthCustomerRepository extends CustomerRepository {

    Optional<Customer> findByEmail(String email);

}
