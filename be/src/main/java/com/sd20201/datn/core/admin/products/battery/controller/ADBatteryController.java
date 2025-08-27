package com.sd20201.datn.core.admin.products.battery.controller;

import com.sd20201.datn.core.admin.products.battery.model.request.ADBatteryRequest;
import com.sd20201.datn.core.admin.products.battery.model.request.ADCreateBatteryRequest;
import com.sd20201.datn.core.admin.products.battery.service.ADBatteryService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_BATTERY)
@RequiredArgsConstructor
public class ADBatteryController {

    private final ADBatteryService adBatteryService;

    // Lấy danh sách pin (phân trang + filter)
    @GetMapping
    public ResponseEntity<?> getAllBatteries(@ModelAttribute ADBatteryRequest request) {
        return Helper.createResponseEntity(adBatteryService.getAllBatteries(request));
    }

    // Thêm mới pin
    @PostMapping("/add")
    public ResponseEntity<?> createBattery(@RequestBody ADCreateBatteryRequest request) {
        return Helper.createResponseEntity(adBatteryService.createBattery(request));
    }

    // Cập nhật pin theo id
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBattery(
            @PathVariable String id,
            @RequestBody ADCreateBatteryRequest request
    ) {
        return Helper.createResponseEntity(adBatteryService.updateBattery(id, request));
    }

    // Cập nhật trạng thái ACTIVE/INACTIVE theo id
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateBatteryStatus(@PathVariable String id) {
        return Helper.createResponseEntity(adBatteryService.updateBatteryStatus(id));
    }
}
