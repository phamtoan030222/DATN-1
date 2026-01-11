package com.sd20201.datn.infrastructure.secutiry.repository;

import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.repository.CustomerRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthCustomerRepository extends CustomerRepository {

    @Query(value = """
    SELECT c
        from Customer c JOIN Account a on c.account.id = a.id
        WHERE a.username = :username
    """)
    Optional<Customer> findByUsername(@Param("username") String username);

    Optional<Customer> findByEmail(String email);

}
