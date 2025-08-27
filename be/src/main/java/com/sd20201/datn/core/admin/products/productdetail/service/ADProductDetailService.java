package com.sd20201.datn.core.admin.products.productdetail.service;

import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADProductDetailService {


    ResponseObject<?> getProductDetails(ADPDProductDetailRequest request);

    ResponseObject<?> getColors();

    ResponseObject<?> getRAMs();

    ResponseObject<?> getCPUs();

    ResponseObject<?> getHardDrivers();

    ResponseObject<?> getGPUs();

    ResponseObject<?> getMaterials();

    ResponseObject<?> getDetail(String id);

    ResponseObject<?> changeStatus(String id);

    ResponseObject<?> modify(ADPDProductDetailCreateUpdateRequest request);

}
