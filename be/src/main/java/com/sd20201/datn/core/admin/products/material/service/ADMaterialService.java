package com.sd20201.datn.core.admin.products.material.service;

import com.sd20201.datn.core.admin.products.material.model.request.ADCreateMaterialRequest;
import com.sd20201.datn.core.admin.products.material.model.request.ADMaterialRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADMaterialService {

    ResponseObject<?> getAllMaterials(ADMaterialRequest request);

    ResponseObject<?> createMaterial(ADCreateMaterialRequest request);

    ResponseObject<?> updateMaterialStatus(String id);

    ResponseObject<?> updateMaterial(String id, ADCreateMaterialRequest request);

}
