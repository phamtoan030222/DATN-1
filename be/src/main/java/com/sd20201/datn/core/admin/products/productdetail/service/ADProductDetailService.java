package com.sd20201.datn.core.admin.products.productdetail.service;

import com.sd20201.datn.core.admin.products.productdetail.model.request.ADAddSerialNumberRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDExistVariantRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADQuickAddProductRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDVariantRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    ResponseObject<?> createVariant(String idProduct,ADPDVariantRequest variant);

    ResponseObject<?> isIMEIExist(List<String> ids);

    ResponseObject<?> quickAddPropertiesProduct(Map<String, ?> request);

    ResponseObject<?> getMinMaxPrice();

    ResponseObject<?> getImeiProductDetail(String idProductDetail);

    ResponseObject<?> changeStatusImei(String idImei);

    ResponseObject<?> update(ADPDProductDetailCreateUpdateRequest request);

    ResponseObject<?> saveImage(MultipartFile file);

    ResponseObject<?> checkExistVariant(ADPDExistVariantRequest request);

    ResponseObject<?> addImeiToExistProductDetail(ADAddSerialNumberRequest request);
}
