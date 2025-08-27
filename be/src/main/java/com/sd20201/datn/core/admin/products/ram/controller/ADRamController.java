package com.sd20201.datn.core.admin.products.ram.controller;

import com.sd20201.datn.core.admin.products.ram.model.request.ADCreateRam;
import com.sd20201.datn.core.admin.products.ram.model.request.ADRamRequest;
import com.sd20201.datn.core.admin.products.ram.service.ADRamService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_RAM)
@RequiredArgsConstructor
public class ADRamController {

    private final ADRamService adRamService;

    @GetMapping
    public ResponseEntity<?> getAllRam(@ModelAttribute ADRamRequest request){
        return Helper.createResponseEntity(adRamService.getAllRam(request));
    }

    @PostMapping("/add")
    public ResponseEntity<?> createRam(@RequestBody ADCreateRam  request){
        return Helper.createResponseEntity(adRamService.createRam(request));
    }

    // Nếu muốn: Cập nhật RAM theo id
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRam(@PathVariable String id, @RequestBody ADCreateRam request) {
        return Helper.createResponseEntity(adRamService.updateRam(id, request));
    }

    // Nếu muốn: Cập nhật trạng thái RAM
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateRamStatus(@PathVariable String id) {
        return Helper.createResponseEntity(adRamService.updateRamStatus(id));
    }



}
