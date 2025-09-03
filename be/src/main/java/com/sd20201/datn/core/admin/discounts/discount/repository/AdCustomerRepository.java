package com.sd20201.datn.core.admin.discounts.discount.repository;

import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.repository.CustomerRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface git AdCustomerRepository extends CustomerRepository {
    @Query("SELECT c.email FROM Customer c WHERE c.status = 0 ")
    List<String> findAllActiveCustomerEmails();

}
