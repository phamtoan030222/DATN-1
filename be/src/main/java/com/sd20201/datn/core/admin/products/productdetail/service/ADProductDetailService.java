package com.sd20201.datn.core.admin.products.productdetail.service;

import com.sd20201.datn.core.admin.products.product.model.request.ADQuickAddProductRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDVariantRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.infrastructure.constant.ProductPropertiesType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    ResponseObject<?> createVariant(String idProduct,ADPDVariantRequest variant,List<MultipartFile> images);

    ResponseObject<?> isIMEIExist(List<String> ids);

    ResponseObject<?> quickAddPropertiesProduct(ADQuickAddProductRequest request);

    ResponseObject<?> getMinMaxPrice();

    ResponseObject<?> getImeiProductDetail(String idProductDetail);

    ResponseObject<?> changeStatusImei(String idImei);
}
