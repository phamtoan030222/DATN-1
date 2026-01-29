package com.sd20201.datn.core.client.products.productdetail.repository;

import com.sd20201.datn.entity.Product;
import com.sd20201.datn.repository.ProductRepository;

import java.util.Optional;

public interface ClientPDProductRepository extends ProductRepository {

    Optional<Product> findByCode(String code);

}
