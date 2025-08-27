package com.sd20201.datn.core.admin.products.cpu.service;

import com.sd20201.datn.core.admin.products.cpu.model.request.ADProductCPUCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.cpu.model.request.ADProductCPURequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADProductCPUService {
    ResponseObject<?> getCPUs(ADProductCPURequest request);

    ResponseObject<?> modify(ADProductCPUCreateUpdateRequest request);

    ResponseObject<?> getDetail(String id);

    ResponseObject<?> changeStatus(String id);
}
