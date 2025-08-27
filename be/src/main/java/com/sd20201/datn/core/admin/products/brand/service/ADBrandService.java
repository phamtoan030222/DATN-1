package com.sd20201.datn.core.admin.products.brand.service;

import com.sd20201.datn.core.admin.products.brand.model.request.ADBrandRequest;
import com.sd20201.datn.core.admin.products.brand.model.request.ADCreateBrandRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADBrandService {
    ResponseObject<?> getAllBrands(ADBrandRequest request);
    ResponseObject<?> createBrand(ADCreateBrandRequest request);
    ResponseObject<?> updateBrandStatus(String id);
    ResponseObject<?> updateBrand(String id, ADCreateBrandRequest request);
}

