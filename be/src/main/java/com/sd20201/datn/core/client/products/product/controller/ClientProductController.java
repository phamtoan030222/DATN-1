package com.sd20201.datn.core.client.products.product.controller;

import com.sd20201.datn.core.client.products.product.model.request.ClientProductCreateUpdateRequest;
import com.sd20201.datn.core.client.products.product.model.request.ClientProductRequest;
import com.sd20201.datn.core.client.products.product.service.ClientProductService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping(MappingConstants.API_PREFIX_ORDER_ONLINE_PRODUCT)
@RequiredArgsConstructor
public class ClientProductController {

    private final ClientProductService productService;

    @GetMapping
    ResponseEntity<?> getProducts(ClientProductRequest request) {
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
    ResponseEntity<?> modifyProduct(@RequestPart ClientProductCreateUpdateRequest request, @RequestPart List<MultipartFile> images) {
        return Helper.createResponseEntity(productService.modify(request, images));
    }

    @PutMapping
    ResponseEntity<?> updateProduct(@RequestBody ClientProductCreateUpdateRequest request) {
        return Helper.createResponseEntity(productService.update(request));
    }

    @GetMapping("/combobox")
    ResponseEntity<?> getProductCombobox() {
        return Helper.createResponseEntity(productService.getProductsCombobox());
    }
}
