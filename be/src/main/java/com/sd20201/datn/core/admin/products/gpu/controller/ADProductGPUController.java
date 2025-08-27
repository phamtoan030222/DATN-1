package com.sd20201.datn.core.admin.products.gpu.controller;

import com.sd20201.datn.core.admin.products.gpu.model.request.ADProductGPUCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.gpu.model.request.ADProductGPURequest;
import com.sd20201.datn.core.admin.products.gpu.service.ADProductGPUService;
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
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_GPU)
@RequiredArgsConstructor
public class ADProductGPUController {

    private final ADProductGPUService gpuService;

    @GetMapping
    ResponseEntity<?> getGPUs(ADProductGPURequest request) {
        return Helper.createResponseEntity(gpuService.getGPUs(request));
    }

    @GetMapping("/{id}")
    ResponseEntity<?> getGPU(@PathVariable String id) {
        return Helper.createResponseEntity(gpuService.getDetail(id));
    }

    @GetMapping("/change-status/{id}")
    ResponseEntity<?> changeStatus(@PathVariable String id) {
        return Helper.createResponseEntity(gpuService.changeStatus(id));
    }

    @PostMapping
    ResponseEntity<?> modifyGpu(@RequestBody ADProductGPUCreateUpdateRequest request) {
        return Helper.createResponseEntity(gpuService.modify(request));
    }

}
