package com.sd20201.datn.core.admin.products.screen.service;

import com.sd20201.datn.core.admin.products.screen.model.request.ADProductScreenCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.screen.model.request.ADProductScreenRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADProductScreenService {
    ResponseObject<?> getScreens(ADProductScreenRequest request);

    ResponseObject<?> getDetail(String id);

    ResponseObject<?> changeStatus(String id);

    ResponseObject<?> modify(ADProductScreenCreateUpdateRequest request);
}
