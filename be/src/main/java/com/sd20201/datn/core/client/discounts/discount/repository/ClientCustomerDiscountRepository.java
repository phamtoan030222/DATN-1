package com.sd20201.datn.core.client.discounts.discount.repository;

import com.sd20201.datn.repository.CustomerRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ClientCustomerDiscountRepository extends CustomerRepository {
    @Query("SELECT c.email FROM Customer c WHERE c.status = 0 ")
    List<String> findAllActiveCustomerEmails();

}
