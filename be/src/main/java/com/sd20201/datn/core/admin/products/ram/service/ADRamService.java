package com.sd20201.datn.core.admin.products.ram.service;

import com.sd20201.datn.core.admin.products.ram.model.request.ADCreateRam;
import com.sd20201.datn.core.admin.products.ram.model.request.ADRamRequest;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import jakarta.validation.Valid;

public interface ADRamService {

    ResponseObject<?> getAllRam(ADRamRequest request);

    ResponseObject<?> createRam(@Valid ADCreateRam request);

    ResponseObject<?> updateRamStatus(String id);

    ResponseObject<?> updateRam(String id, @Valid ADCreateRam request);


}
