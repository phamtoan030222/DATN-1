package com.sd20201.datn.core.admin.products.material.controller;

import com.sd20201.datn.core.admin.products.material.model.request.ADCreateMaterialRequest;
import com.sd20201.datn.core.admin.products.material.model.request.ADMaterialRequest;
import com.sd20201.datn.core.admin.products.material.service.ADMaterialService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_MATERIAL)
@RequiredArgsConstructor
public class ADMaterialController {

    private final ADMaterialService  adMaterialService;

    @GetMapping
    public ResponseEntity<?> getAllMaterials(@ModelAttribute ADMaterialRequest request) {
        return Helper.createResponseEntity(adMaterialService.getAllMaterials(request));
    }

    // Thêm mới chất liệu
    @PostMapping("/add")
    public ResponseEntity<?> createMaterial(@RequestBody ADCreateMaterialRequest request) {
        return Helper.createResponseEntity(adMaterialService.createMaterial(request));
    }

    // Cập nhật chất liệu theo id
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMaterial(@PathVariable String id, @RequestBody ADCreateMaterialRequest request) {
        return Helper.createResponseEntity(adMaterialService.updateMaterial(id, request));
    }

    // Cập nhật trạng thái ACTIVE/INACTIVE theo id
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateMaterialStatus(@PathVariable String id) {
        return Helper.createResponseEntity(adMaterialService.updateMaterialStatus(id));
    }
}