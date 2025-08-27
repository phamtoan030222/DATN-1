package com.sd20201.datn.core.admin.products.material.service.impl;

import com.sd20201.datn.core.admin.products.material.model.request.ADCreateMaterialRequest;
import com.sd20201.datn.core.admin.products.material.model.request.ADMaterialRequest;
import com.sd20201.datn.core.admin.products.material.repository.ADMaterialRepository;
import com.sd20201.datn.core.admin.products.material.service.ADMaterialService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Material;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ADMaterialServiceImpl implements ADMaterialService {

    private final ADMaterialRepository adMaterialRepository;

    @Override
    public ResponseObject<?> getAllMaterials(ADMaterialRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");
        return new ResponseObject<>(
                PageableObject.of(adMaterialRepository.getAllMaterials(pageable, request.getKey())),
                HttpStatus.OK,
                "Lấy thành công danh sách chất liệu"
        );
    }

    @Override
    public ResponseObject<?> createMaterial(ADCreateMaterialRequest request) {
        Material material = new Material();
        material.setCode(request.getCode());
        material.setTopCaseMaterial(request.getTopCaseMaterial());
        material.setBottomCaseMaterial(request.getBottomCaseMaterial());
        material.setKeyboardMaterial(request.getKeyboardMaterial());
        material.setStatus(EntityStatus.ACTIVE);
        material.setCreatedDate(System.currentTimeMillis());

        adMaterialRepository.save(material);

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Thêm mới chất liệu thành công",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> updateMaterialStatus(String id) {
        Optional<Material> optionalMaterial = adMaterialRepository.findById(id);
        if (optionalMaterial.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Chất liệu không tồn tại", false, null);
        }

        Material material = optionalMaterial.get();
        material.setStatus(material.getStatus().equals(EntityStatus.ACTIVE) ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
        adMaterialRepository.save(material);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái chất liệu thành công", true, null);
    }

    @Override
    public ResponseObject<?> updateMaterial(String id, ADCreateMaterialRequest request) {
        Optional<Material> optionalMaterial = adMaterialRepository.findById(id);
        if (optionalMaterial.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Chất liệu không tồn tại", false, null);
        }

        Material material = optionalMaterial.get();
        material.setCode(request.getCode());
        material.setTopCaseMaterial(request.getTopCaseMaterial());
        material.setBottomCaseMaterial(request.getBottomCaseMaterial());
        material.setKeyboardMaterial(request.getKeyboardMaterial());

        adMaterialRepository.save(material);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật chất liệu thành công", true, null);
    }

}
