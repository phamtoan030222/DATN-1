package com.sd20201.datn.core.admin.products.productdetail.controller;

import com.sd20201.datn.core.admin.products.product.model.request.ADProductCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.service.ADProductDetailService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_DETAIL)
@RequiredArgsConstructor
public class ADProductDetailController {

    private final ADProductDetailService productDetailService;

    @GetMapping
    ResponseEntity<?> getScreens(ADPDProductDetailRequest request) {
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
    ResponseEntity<?> getScreen(@PathVariable String id) {
        return Helper.createResponseEntity(productDetailService.getDetail(id));
    }

    @GetMapping("/change-status/{id}")
    ResponseEntity<?> changeStatus(@PathVariable String id) {
        return Helper.createResponseEntity(productDetailService.changeStatus(id));
    }

    @PostMapping
    ResponseEntity<?> modifyScreen(@RequestBody ADPDProductDetailCreateUpdateRequest request) {
        return Helper.createResponseEntity(productDetailService.modify(request));
    }

}
