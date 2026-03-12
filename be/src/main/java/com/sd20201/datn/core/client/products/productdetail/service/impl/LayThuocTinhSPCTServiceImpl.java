package com.sd20201.datn.core.client.products.productdetail.service.impl;

import com.sd20201.datn.core.client.products.productdetail.repository.ClientPDCPURepository;
import com.sd20201.datn.core.client.products.productdetail.repository.ClientPDColorRepository;
import com.sd20201.datn.core.client.products.productdetail.repository.ClientPDGPURepository;
import com.sd20201.datn.core.client.products.productdetail.repository.ClientPDHardDriveRepository;
import com.sd20201.datn.core.client.products.productdetail.repository.ClientPDRAMRepository;
import com.sd20201.datn.core.client.products.productdetail.service.LayThuocTinhSPCTVervice;
import com.sd20201.datn.core.common.base.ResponseObject;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LayThuocTinhSPCTServiceImpl implements LayThuocTinhSPCTVervice {
    private final ClientPDColorRepository color;
    private final ClientPDCPURepository cpu;
    private final ClientPDGPURepository gpu;
    private final ClientPDRAMRepository ram;
    private final ClientPDHardDriveRepository hardDrive;

    @Override
    public ResponseObject<?> getColorsByPD(String pdId) {
        return ResponseObject.successForward(color.getColorByPD(pdId), "OKE");
    }

    @Override
    public ResponseObject<?> getCpuByPD(String cpuId) {
        return ResponseObject.successForward(cpu.getCpuByPD(cpuId), "OKE");
    }

    @Override
    public ResponseObject<?> getGpuByPD(String gpuId) {
        return ResponseObject.successForward(gpu.getCpuByPD(gpuId), "OKE");
    }

    @Override
    public ResponseObject<?> getHardDriveByPD(String hardDriveId) {
        return ResponseObject.successForward(hardDrive.getCpuByPD(hardDriveId), "OKE");
    }

    @Override
    public ResponseObject<?> getRamByPB(String ramId) {
        return ResponseObject.successForward(ram.getCpuByPD(ramId), "OKE");
    }
}