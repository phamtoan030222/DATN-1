package com.sd20201.datn.core.admin.products.brand.controller;

import com.sd20201.datn.core.admin.products.brand.model.request.ADBrandRequest;
import com.sd20201.datn.core.admin.products.brand.model.request.ADCreateBrandRequest;
import com.sd20201.datn.core.admin.products.brand.service.ADBrandService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_BRAND)
@RequiredArgsConstructor
public class ADBrandController {

    private final ADBrandService adBrandService;

    @GetMapping
    public ResponseEntity<?> getAllBrands(@ModelAttribute ADBrandRequest request) {
        return Helper.createResponseEntity(adBrandService.getAllBrands(request));
    }

    // Thêm mới thương hiệu
    @PostMapping("/add")
    public ResponseEntity<?> createBrand(@RequestBody ADCreateBrandRequest request) {
        return Helper.createResponseEntity(adBrandService.createBrand(request));
    }

    // Cập nhật thương hiệu theo id
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBrand(@PathVariable String id, @RequestBody ADCreateBrandRequest request) {
        return Helper.createResponseEntity(adBrandService.updateBrand(id, request));
    }

    // Cập nhật trạng thái ACTIVE/INACTIVE theo id
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateBrandStatus(@PathVariable String id) {
        return Helper.createResponseEntity(adBrandService.updateBrandStatus(id));
    }
}
