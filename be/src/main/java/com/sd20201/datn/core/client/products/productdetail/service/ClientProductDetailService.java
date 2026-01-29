package com.sd20201.datn.core.client.products.productdetail.service;

import com.sd20201.datn.core.client.products.productdetail.model.request.ClientAddSerialNumberRequest;
import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDExistVariantRequest;
import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDProductDetailCreateUpdateRequest;
import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDProductDetailRequest;
import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDVariantRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ClientProductDetailService {


    ResponseObject<?> getProductDetails(ClientPDProductDetailRequest request);

    ResponseObject<?> getColors();

    ResponseObject<?> getRAMs();

    ResponseObject<?> getCPUs();

    ResponseObject<?> getHardDrivers();

    ResponseObject<?> getGPUs();

    ResponseObject<?> getMaterials();

    ResponseObject<?> getDetail(String id);

    ResponseObject<?> changeStatus(String id);

    ResponseObject<?> modify(ClientPDProductDetailCreateUpdateRequest request);

    ResponseObject<?> createVariant(String idProduct, ClientPDVariantRequest variant);

    ResponseObject<?> isIMEIExist(List<String> ids);

    ResponseObject<?> quickAddPropertiesProduct(Map<String, ?> request);

    ResponseObject<?> getMinMaxPrice();

    ResponseObject<?> getImeiProductDetail(String idProductDetail);

    ResponseObject<?> changeStatusImei(String idImei);

    ResponseObject<?> update(ClientPDProductDetailCreateUpdateRequest request);

    ResponseObject<?> saveImage(MultipartFile file);

    ResponseObject<?> checkExistVariant(ClientPDExistVariantRequest request);

    ResponseObject<?> addImeiToExistProductDetail(ClientAddSerialNumberRequest request);
}
