package com.sd20201.datn.core.admin.products.color.service;

import com.sd20201.datn.core.admin.products.color.model.request.AdColorRequest;
import com.sd20201.datn.core.admin.products.color.model.request.ColorCreateUpdateRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import jakarta.validation.Valid;

public interface AdColorService {

    ResponseObject<?> getAllColors(AdColorRequest request);

    ResponseObject<?> createColor(@Valid ColorCreateUpdateRequest request);

    ResponseObject<?> updateColor(String id, @Valid ColorCreateUpdateRequest request);


}
