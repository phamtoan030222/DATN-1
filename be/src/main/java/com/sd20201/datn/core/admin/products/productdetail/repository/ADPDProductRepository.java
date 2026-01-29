package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.entity.Product;
import com.sd20201.datn.repository.ProductRepository;

import java.util.Optional;

public interface ADPDProductRepository extends ProductRepository {

    Optional<Product> findByCode(String code);

}