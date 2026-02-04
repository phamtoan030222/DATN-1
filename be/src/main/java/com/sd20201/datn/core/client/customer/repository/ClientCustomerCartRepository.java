package com.sd20201.datn.core.client.customer.repository;

import com.sd20201.datn.entity.Cart;
import com.sd20201.datn.repository.CartRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClientCustomerCartRepository extends CartRepository {

    @Query("SELECT c FROM Cart c where c.customer.id =  :customerId")
    Optional<Cart> findByCustomerId(String customerId);

}
