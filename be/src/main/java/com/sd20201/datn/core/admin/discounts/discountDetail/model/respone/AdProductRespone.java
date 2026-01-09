package com.sd20201.datn.core.admin.discounts.discountDetail.model.respone;

import com.sd20201.datn.repository.ProductRepository;

public interface AdProductRespone {

    String getId();

    String getProductCode();

    String getProductName();

    String getProductBrand();

    Long getCreatedDate();

    Long getQuantity();
}
