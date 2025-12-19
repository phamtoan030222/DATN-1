package com.sd20201.datn.core.admin.products.product.service;

import com.sd20201.datn.core.admin.products.product.model.request.ADProductCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.product.model.request.ADProductRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ADProductService {

    ResponseObject<?> getProducts(ADProductRequest request);

    ResponseObject<?> getDetail(String id);

    ResponseObject<?> changeStatus(String id);

    ResponseObject<?> modify(ADProductCreateUpdateRequest request, List<MultipartFile> images);

    ResponseObject<?> getScreens();

    ResponseObject<?> getBrands();

    ResponseObject<?> getBatteries();

    ResponseObject<?> getOperatingSystems();

    ResponseObject<?> update(ADProductCreateUpdateRequest request);
}
