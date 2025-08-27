package com.sd20201.datn.core.admin.products.color.service.impl;

import com.sd20201.datn.core.admin.products.color.model.request.AdColorRequest;
import com.sd20201.datn.core.admin.products.color.model.request.ColorCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.color.repository.AdColorRepository;
import com.sd20201.datn.core.admin.products.color.service.AdColorService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Color;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AdColorServiceImpl implements AdColorService {

    private final AdColorRepository adColorRepository;

    @Override
    public ResponseObject<?> getAllColors(AdColorRequest request) {
        Pageable pageable =  Helper.createPageable(request, "createdDate");

        return new ResponseObject<>(
                PageableObject.of(adColorRepository.getAllColors(pageable, request.getColorName(), request.getColorStatus())),
                HttpStatus.OK,
                "Lấy thành công danh sách màu "
        );
    }

    @Override
    public ResponseObject<?> createColor(ColorCreateUpdateRequest request) {

        // Kiểm tra trùng tên
        List<Color> colorsByName = adColorRepository.findAllByName(request.getColorName());
        if (!colorsByName.isEmpty()) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Tên màu sắc đã tồn tại",
                    false,
                    "COLOR_NAME_EXISTS"
            );
        }

        // Kiểm tra trùng code
        List<Color> colorsByCode = adColorRepository.findAllByCode(request.getColorCode());
        if (!colorsByCode.isEmpty()) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Mã màu đã tồn tại",
                    false,
                    "COLOR_CODE_EXISTS"
            );
        }

        // Tạo mới màu
        Color color = new Color();
        color.setName(request.getColorName());
        color.setCode(request.getColorCode());
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
        return null;
    }
}
