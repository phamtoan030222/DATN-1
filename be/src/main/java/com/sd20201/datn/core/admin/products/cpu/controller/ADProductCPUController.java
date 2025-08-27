package com.sd20201.datn.core.admin.products.cpu.controller;

import com.sd20201.datn.core.admin.products.cpu.model.request.ADProductCPUCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.cpu.model.request.ADProductCPURequest;
import com.sd20201.datn.core.admin.products.cpu.service.ADProductCPUService;
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
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_CPU)
@RequiredArgsConstructor
public class ADProductCPUController {

    private final ADProductCPUService cPUService;

    @GetMapping
    ResponseEntity<?> getCPUs(ADProductCPURequest request) {
        return Helper.createResponseEntity(cPUService.getCPUs(request));
    }

    @GetMapping("/{id}")
    ResponseEntity<?> getCPU(@PathVariable String id) {
        return Helper.createResponseEntity(cPUService.getDetail(id));
    }

    @GetMapping("/change-status/{id}")
    ResponseEntity<?> changeStatus(@PathVariable String id) {
        return Helper.createResponseEntity(cPUService.changeStatus(id));
    }

    @PostMapping
    ResponseEntity<?> modifyCpu(@RequestBody ADProductCPUCreateUpdateRequest request) {
        return Helper.createResponseEntity(cPUService.modify(request));
    }

}
