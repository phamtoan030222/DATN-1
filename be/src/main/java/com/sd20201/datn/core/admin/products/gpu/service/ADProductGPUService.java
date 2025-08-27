package com.sd20201.datn.core.admin.products.gpu.service;

import com.sd20201.datn.core.admin.products.gpu.model.request.ADProductGPUCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.gpu.model.request.ADProductGPURequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADProductGPUService {
    ResponseObject<?> getGPUs(ADProductGPURequest request);

    ResponseObject<?> getDetail(String id);

    ResponseObject<?> changeStatus(String id);

    ResponseObject<?> modify(ADProductGPUCreateUpdateRequest request);
}
