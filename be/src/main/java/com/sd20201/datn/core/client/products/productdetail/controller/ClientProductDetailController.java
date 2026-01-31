package com.sd20201.datn.core.client.products.productdetail.controller;

import com.sd20201.datn.core.client.products.productdetail.model.request.ClientAddSerialNumberRequest;
import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDExistVariantRequest;
import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDProductDetailCreateUpdateRequest;
import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDProductDetailRequest;
import com.sd20201.datn.core.client.products.productdetail.model.request.ClientPDVariantRequest;
import com.sd20201.datn.core.client.products.productdetail.service.ClientProductDetailService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(MappingConstants.API_PREFIX_ORDER_ONLINE_PRODUCT_DETAIL)
@RequiredArgsConstructor
public class ClientProductDetailController {

    private final ClientProductDetailService productDetailService;

    @GetMapping
    ResponseEntity<?> getProductDetails(ClientPDProductDetailRequest request) {
        return Helper.createResponseEntity(productDetailService.getProductDetails(request));
    }

    // combobox property
    @GetMapping("/colors")
    ResponseEntity<?> getColors() {
        return Helper.createResponseEntity(productDetailService.getColors());
    }

    @GetMapping("/rams")
    ResponseEntity<?> getRAMs() {
        return Helper.createResponseEntity(productDetailService.getRAMs());
    }

    @GetMapping("/cpus")
    ResponseEntity<?> getCPUs() {
        return Helper.createResponseEntity(productDetailService.getCPUs());
    }

    @GetMapping("/hard-drives")
    ResponseEntity<?> getHardDrives() {
        return Helper.createResponseEntity(productDetailService.getHardDrivers());
    }

    @GetMapping("/materials")
    ResponseEntity<?> getMaterials() {
        return Helper.createResponseEntity(productDetailService.getMaterials());
    }

    @GetMapping("/gpus")
    ResponseEntity<?> getGPUs() {
        return Helper.createResponseEntity(productDetailService.getGPUs());
    }


    @GetMapping("/{id}")
    ResponseEntity<?> getProductDetail(@PathVariable String id) {
        return Helper.createResponseEntity(productDetailService.getDetail(id));
    }

    @GetMapping("/change-status/{id}")
    ResponseEntity<?> changeStatus(@PathVariable String id) {
        return Helper.createResponseEntity(productDetailService.changeStatus(id));
    }

    @PostMapping("/imei-exists")
    ResponseEntity<?> isIMEIExist(@RequestBody List<String> id) {
        return Helper.createResponseEntity(productDetailService.isIMEIExist(id));
    }


    @GetMapping("/min-max-price")
    ResponseEntity<?> getMinMaxPrice() {
        return Helper.createResponseEntity(productDetailService.getMinMaxPrice());
    }

    @GetMapping("/imei/{idProductDetail}")
    ResponseEntity<?> getImeiProduct(@PathVariable String idProductDetail) {
        return Helper.createResponseEntity(productDetailService.getImeiProductDetail(idProductDetail));
    }

    @GetMapping("/imei/change-status/{idImei}")
    ResponseEntity<?> changeStatusImei(@PathVariable String idImei) {
        return Helper.createResponseEntity(productDetailService.changeStatusImei(idImei));
    }


    @PostMapping("/exist-variant")
    ResponseEntity<?> checkExistVariant(@RequestBody ClientPDExistVariantRequest request) {
        return Helper.createResponseEntity(productDetailService.checkExistVariant(request));
    }

}
