package com.sd20201.datn.core.client.products.productdetail.service;

import com.sd20201.datn.core.client.products.productdetail.model.response.ClientPDPropertyComboboxResponse;
import com.sd20201.datn.core.common.base.ResponseObject;

import java.util.List;

public interface LayThuocTinhSPCTVervice {


    ResponseObject<?> getColorsByPD(String pdId);

    ResponseObject<?> getCpuByPD(String cpuId);

    ResponseObject<?> getGpuByPD(String gpuId);

    ResponseObject<?> getHardDriveByPD(String hardDriveId);

    ResponseObject<?> getRamByPB(String ramId);
}
