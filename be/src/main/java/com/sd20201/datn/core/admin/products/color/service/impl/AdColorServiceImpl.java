package com.sd20201.datn.core.admin.products.color.service.impl;

import com.sd20201.datn.core.admin.products.color.model.request.AdColorRequest;
import com.sd20201.datn.core.admin.products.color.model.request.ColorCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.color.repository.AdColorRepository;
import com.sd20201.datn.core.admin.products.color.service.AdColorService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Brand;
import com.sd20201.datn.entity.Color;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AdColorServiceImpl implements AdColorService {

    private final AdColorRepository adColorRepository;

    @Override
    public ResponseObject<?> getAllColors(AdColorRequest request) {
        Pageable pageable =  Helper.createPageable(request, "createdDate");

        return new ResponseObject<>(
                PageableObject.of(adColorRepository.getAllColors(pageable, request.getKeyword(), request.getStatus())),
                HttpStatus.OK,
                "Lấy thành công danh sách màu "
        );
    }

    @Override
    public ResponseObject<?> createColor(ColorCreateUpdateRequest request) {
        String newName = request.getName() == null ? "" : request.getName().trim();
        String newCode = request.getCode() == null ? "" : request.getCode().trim();

        // Kiểm tra trùng tên
        List<Color> colorsByName = adColorRepository.findAllByName(newName);
        if (!colorsByName.isEmpty()) {
            return ResponseObject.errorForward("Thêm thất bại! Tên màu sắc đã tồn tại.", HttpStatus.CONFLICT);
        }

        // Kiểm tra trùng code
        List<Color> colorsByCode = adColorRepository.findAllByCode(newCode);
        if (!colorsByCode.isEmpty()) {
            return ResponseObject.errorForward("Thêm thất bại! Mã màu đã tồn tại.", HttpStatus.CONFLICT);
        }

        // Tạo mới màu
        Color color = new Color();
        color.setName(newName);
        color.setCode(newCode);
        color.setCreatedDate(System.currentTimeMillis());
        color.setStatus(EntityStatus.ACTIVE);
        adColorRepository.save(color);

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Thêm màu thành công",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> updateColor(String id, ColorCreateUpdateRequest request) {
        Optional<Color> optionalColor = adColorRepository.findById(id);
        if (optionalColor.isEmpty()) {
            return ResponseObject.errorForward("Cập nhật thất bại! Màu sắc không tồn tại.", HttpStatus.NOT_FOUND);
        }

        String newName = request.getName() == null ? "" : request.getName().trim();
        String newCode = request.getCode() == null ? "" : request.getCode().trim();

        // Kiểm tra trùng tên (nhưng BỎ QUA chính cái màu đang được sửa)
        List<Color> colorsByName = adColorRepository.findAllByName(newName);
        boolean isNameDuplicate = colorsByName.stream().anyMatch(c -> !c.getId().equals(id));
        if (isNameDuplicate) {
            return ResponseObject.errorForward("Cập nhật thất bại! Tên màu sắc đã tồn tại ở một bản ghi khác.", HttpStatus.CONFLICT);
        }

        // Kiểm tra trùng code (nhưng BỎ QUA chính cái màu đang được sửa)
        List<Color> colorsByCode = adColorRepository.findAllByCode(newCode);
        boolean isCodeDuplicate = colorsByCode.stream().anyMatch(c -> !c.getId().equals(id));
        if (isCodeDuplicate) {
            return ResponseObject.errorForward("Cập nhật thất bại! Mã màu đã tồn tại ở một bản ghi khác.", HttpStatus.CONFLICT);
        }

        Color color = optionalColor.get();
        color.setCode(newCode);
        color.setName(newName);

        adColorRepository.save(color);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật màu sắc thành công", true, null);
    }

    @Override
    public ResponseObject<?> updateColorStatus(String id) {
        Optional<Color> optionalColor = adColorRepository.findById(id);
        if (optionalColor.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Màu sắc không tồn tại", false, null);
        }

        Color color = optionalColor.get();
        color.setStatus(color.getStatus().equals(EntityStatus.ACTIVE) ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
        adColorRepository.save(color);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái màu sắc thành công", true, null);

    }
}
