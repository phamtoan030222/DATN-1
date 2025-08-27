package com.sd20201.datn.core.admin.products.operating.service;

import com.sd20201.datn.core.admin.products.operating.model.request.ADCreateOperatingRequest;
import com.sd20201.datn.core.admin.products.operating.model.request.ADOperatingRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADOperatingService {

    ResponseObject<?> getAllOperatingSystems(ADOperatingRequest request);

    /**
     * Thêm mới hệ điều hành
     */
    ResponseObject<?> createOperatingSystem(ADCreateOperatingRequest request);

    /**
     * Cập nhật trạng thái hệ điều hành (ACTIVE / INACTIVE)
     */
    ResponseObject<?> updateOperatingSystemStatus(String id);

    /**
     * Cập nhật thông tin hệ điều hành
     */
    ResponseObject<?> updateOperatingSystem(String id, ADCreateOperatingRequest request);

}
