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
                PageableObject.of(adMaterialRepository.getAllMaterials(pageable, request.getKey(), request.getStatus())),
                HttpStatus.OK,
                "Lấy thành công danh sách chất liệu"
        );
    }

    @Override
    public ResponseObject<?> createMaterial(ADCreateMaterialRequest request) {
        // 1. Làm sạch khoảng trắng dữ liệu đầu vào
        String top = request.getTopCaseMaterial() == null ? "" : request.getTopCaseMaterial().trim();
        String bottom = request.getBottomCaseMaterial() == null ? "" : request.getBottomCaseMaterial().trim();
        String keyboard = request.getKeyboardMaterial() == null ? "" : request.getKeyboardMaterial().trim();

        // 2. Check trùng (Giả sử bạn đã tạo hàm checkDuplicate có LOWER và TRIM trong Repository)
        // Nếu chưa tạo hàm này, bạn có thể tạm dùng hàm findBy... cũ của bạn
        Optional<Material> existingMaterial = adMaterialRepository.checkDuplicate(top, bottom, keyboard);

        if (existingMaterial.isPresent()) {
            return ResponseObject.errorForward("Thêm thất bại! Tổ hợp chất liệu này đã tồn tại.", HttpStatus.CONFLICT);
        }

        Material material = new Material();
        material.setCode(request.getCode());
        material.setTopCaseMaterial(top);
        material.setBottomCaseMaterial(bottom);
        material.setKeyboardMaterial(keyboard);
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
        System.out.println(material.getCode());
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

        // 1. Làm sạch khoảng trắng
        String top = request.getTopCaseMaterial() == null ? "" : request.getTopCaseMaterial().trim();
        String bottom = request.getBottomCaseMaterial() == null ? "" : request.getBottomCaseMaterial().trim();
        String keyboard = request.getKeyboardMaterial() == null ? "" : request.getKeyboardMaterial().trim();

        // 2. Check trùng
        Optional<Material> existingMaterial = adMaterialRepository.checkDuplicate(top, bottom, keyboard);

        // QUAN TRỌNG: Phải check xem cái bị trùng có phải là CÁI ĐANG SỬA không (!id.equals)
        if (existingMaterial.isPresent() && !existingMaterial.get().getId().equals(id)) {
            return ResponseObject.errorForward("Cập nhật thất bại! Tổ hợp chất liệu này bị trùng lặp với một bản ghi khác.", HttpStatus.CONFLICT);
        }

        Material material = optionalMaterial.get();
        material.setTopCaseMaterial(top);
        material.setBottomCaseMaterial(bottom);
        material.setKeyboardMaterial(keyboard);

        adMaterialRepository.save(material);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật chất liệu thành công", true, null);
    }
}