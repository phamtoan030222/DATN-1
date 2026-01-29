package com.sd20201.datn.core.client.products.product.service;

import com.sd20201.datn.core.client.products.product.model.request.ClientProductCreateUpdateRequest;
import com.sd20201.datn.core.client.products.product.model.request.ClientProductRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ClientProductService {

    ResponseObject<?> getProducts(ClientProductRequest request);

    ResponseObject<?> getDetail(String id);

    ResponseObject<?> changeStatus(String id);

    ResponseObject<?> modify(ClientProductCreateUpdateRequest request, List<MultipartFile> images);

    ResponseObject<?> getScreens();

    ResponseObject<?> getBrands();

    ResponseObject<?> getBatteries();

    ResponseObject<?> getOperatingSystems();

    ResponseObject<?> update(ClientProductCreateUpdateRequest request);

    ResponseObject<?> getProductsCombobox();
}
