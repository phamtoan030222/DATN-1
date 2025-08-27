package com.sd20201.datn.core.admin.products.gpu.service.impl;

import com.sd20201.datn.core.admin.products.gpu.model.request.ADProductGPUCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.gpu.model.request.ADProductGPURequest;
import com.sd20201.datn.core.admin.products.gpu.repository.ADProductGPURepository;
import com.sd20201.datn.core.admin.products.gpu.service.ADProductGPUService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.GPU;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ADProductGPUServiceImpl implements ADProductGPUService {

    private final ADProductGPURepository gpuRepository;

    @Override
    public ResponseObject<?> getGPUs(ADProductGPURequest request) {
        return ResponseObject.successForward(
                PageableObject.of(gpuRepository.getGPUs(Helper.createPageable(request), request)),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getDetail(String id) {
        return gpuRepository.getGPUById(id).map(gpu -> ResponseObject.successForward(gpu, "Get detail gpu successfully"))
                .orElse(ResponseObject.errorForward("Get detail fail!!!", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> changeStatus(String id) {
        return gpuRepository.findById(id)
                .map(gpu -> {
                    gpu.setStatus(gpu.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
                    gpuRepository.save(gpu);
                    return ResponseObject.successForward(null, "Update status gpu successfully");
                })
                .orElse(ResponseObject.errorForward("Update status gpu failure!!! GPU not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> modify(ADProductGPUCreateUpdateRequest request) {
        return request.getId() == null || request.getId().isEmpty()  ? create(request) : update(request);
    }

    private ResponseObject<?> update(ADProductGPUCreateUpdateRequest request) {
        Optional<GPU> optionalGPU = gpuRepository.findById(request.getId());

        if(optionalGPU.isEmpty()) return ResponseObject.errorForward("Update fail!!! GPU not found", HttpStatus.NOT_FOUND);

        GPU gpu = optionalGPU.get();

        gpu.setName(request.getName());
        gpu.setCode(request.getCode());
        gpu.setDescription(request.getDescription());
        gpu.setBrand(request.getBrand());
        gpu.setGeneration(request.getGeneration());
        gpu.setSeries(request.getSeries());
        gpu.setReleaseYear(request.getReleaseYear());

        return ResponseObject.successForward(gpuRepository.save(gpu), "GPU updated successfully");
    }

    private ResponseObject<?> create(ADProductGPUCreateUpdateRequest request) {
        GPU gpu = new GPU();

        gpu.setName(request.getName());
        gpu.setCode(request.getCode());
        gpu.setDescription(request.getDescription());
        gpu.setBrand(request.getBrand());
        gpu.setGeneration(request.getGeneration());
        gpu.setSeries(request.getSeries());
        gpu.setReleaseYear(request.getReleaseYear());
        return ResponseObject.successForward(gpuRepository.save(gpu), "GPU created successfully");
    }
}
