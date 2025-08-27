package com.sd20201.datn.core.admin.products.product.controller;

import com.sd20201.datn.core.admin.products.product.model.request.ADProductCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.product.model.request.ADProductRequest;
import com.sd20201.datn.core.admin.products.product.service.ADProductService;
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
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS)
@RequiredArgsConstructor
public class ADProductController {

    private final ADProductService productService;

    @GetMapping
    ResponseEntity<?> getScreens(ADProductRequest request) {
        return Helper.createResponseEntity(productService.getProducts(request));
    }

    // combobox property
    @GetMapping("/screens")
    ResponseEntity<?> getScreens() {
        return Helper.createResponseEntity(productService.getScreens());
    }

    @GetMapping("/batteries")
    ResponseEntity<?> getBatteries() {
        return Helper.createResponseEntity(productService.getBatteries());
    }

    @GetMapping("/operating-systems")
    ResponseEntity<?> getOperatingSystems() {
        return Helper.createResponseEntity(productService.getOperatingSystems());
    }

    @GetMapping("/brands")
    ResponseEntity<?> getBrands() {
        return Helper.createResponseEntity(productService.getBrands());
    }

    @GetMapping("/{id}")
    ResponseEntity<?> getScreen(@PathVariable String id) {
        return Helper.createResponseEntity(productService.getDetail(id));
    }

    @GetMapping("/change-status/{id}")
    ResponseEntity<?> changeStatus(@PathVariable String id) {
        return Helper.createResponseEntity(productService.changeStatus(id));
    }

    @PostMapping
    ResponseEntity<?> modifyScreen(@RequestBody ADProductCreateUpdateRequest request) {
        return Helper.createResponseEntity(productService.modify(request));
    }

}
