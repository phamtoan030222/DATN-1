package com.sd20201.datn.core.admin.products.cpu.service.impl;

import com.sd20201.datn.core.admin.products.cpu.model.request.ADProductCPUCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.cpu.model.request.ADProductCPURequest;
import com.sd20201.datn.core.admin.products.cpu.repository.ADProductCPURepository;
import com.sd20201.datn.core.admin.products.cpu.service.ADProductCPUService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.CPU;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ADProductCPUServiceImpl implements ADProductCPUService {

    private final ADProductCPURepository cpuRepository;

    @Override
    public ResponseObject<?> getCPUs(ADProductCPURequest request) {
        return new ResponseObject(
                PageableObject.of(cpuRepository.getCPUs(Helper.createPageable(request, "created_date"), request)),
                HttpStatus.OK,
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> modify(ADProductCPUCreateUpdateRequest request) {
        return new ResponseObject(request.getId() == null || request.getId().isEmpty() ? createCPU(request) : updateCPU(request), HttpStatus.OK, "OKE");
    }

    private Object updateCPU(ADProductCPUCreateUpdateRequest request) {
        Optional<CPU> optionalCPU = cpuRepository.findById(request.getId());

        if(optionalCPU.isEmpty()) return ResponseObject.errorForward("Update fail!!! CPU not found", HttpStatus.NOT_FOUND);

        CPU cpu = optionalCPU.get();

        cpu.setName(request.getName());
        cpu.setCode(request.getCode());
        cpu.setDescription(request.getDescription());
        cpu.setBrand(request.getBrand());
        cpu.setGeneration(request.getGeneration());
        cpu.setSeries(request.getSeries());
        cpu.setReleaseYear(request.getReleaseYear());

        return ResponseObject.successForward(cpuRepository.save(cpu), "CPU updated successfully");
    }

    private Object createCPU(ADProductCPUCreateUpdateRequest request) {

        Optional<CPU> optionalCPU = cpuRepository.getCPUByCode(request.getCode());

        if(optionalCPU.isPresent()) return ResponseObject.errorForward("Create fail!!! CPU code is existed", HttpStatus.NOT_FOUND);

        CPU cpu = new CPU();

        cpu.setName(request.getName());
        cpu.setCode(request.getCode());
        cpu.setDescription(request.getDescription());
        cpu.setBrand(request.getBrand());
        cpu.setGeneration(request.getGeneration());
        cpu.setSeries(request.getSeries());
        cpu.setReleaseYear(request.getReleaseYear());
        return ResponseObject.successForward(cpuRepository.save(cpu), "CPU created successfully");
    }

    @Override
    public ResponseObject<?> getDetail(String id) {
        return cpuRepository.getCPUById(id).map(cpu -> ResponseObject.successForward(cpu, "Get detail cpu successfully"))
                .orElse(ResponseObject.errorForward("Get detail fail!!!", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> changeStatus(String id) {
        return cpuRepository.findById(id)
                .map(cpu -> {
                    cpu.setStatus(cpu.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
                    cpuRepository.save(cpu);
                    return ResponseObject.successForward(null, "Update status cpu successfully");
                })
                .orElse(ResponseObject.errorForward("Update status cpu failure!!! CPU not found", HttpStatus.NOT_FOUND));
    }
}
