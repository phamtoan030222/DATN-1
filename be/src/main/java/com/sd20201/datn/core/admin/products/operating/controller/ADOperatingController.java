package com.sd20201.datn.core.admin.products.operating.controller;

import com.sd20201.datn.core.admin.products.operating.model.request.ADCreateOperatingRequest;
import com.sd20201.datn.core.admin.products.operating.model.request.ADOperatingRequest;
import com.sd20201.datn.core.admin.products.operating.service.ADOperatingService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_OPERATING)
public class ADOperatingController {

    private final ADOperatingService adOperatingService;

    @GetMapping
    public ResponseEntity<?> getAllOperatingSystems(@ModelAttribute ADOperatingRequest request) {
        return Helper.createResponseEntity(adOperatingService.getAllOperatingSystems(request));
    }

    // Thêm mới hệ điều hành
    @PostMapping("/add")
    public ResponseEntity<?> createOperatingSystem(@RequestBody ADCreateOperatingRequest request) {
        return Helper.createResponseEntity(adOperatingService.createOperatingSystem(request));
    }

    // Cập nhật hệ điều hành theo id
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOperatingSystem(@PathVariable String id,
                                                   @RequestBody ADCreateOperatingRequest request) {
        return Helper.createResponseEntity(adOperatingService.updateOperatingSystem(id, request));
    }

    // Cập nhật trạng thái ACTIVE/INACTIVE theo id
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOperatingSystemStatus(@PathVariable String id) {
        return Helper.createResponseEntity(adOperatingService.updateOperatingSystemStatus(id));
    }
}
