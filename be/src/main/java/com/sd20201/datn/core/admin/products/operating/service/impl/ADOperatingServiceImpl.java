package com.sd20201.datn.core.admin.products.operating.service.impl;

import com.sd20201.datn.core.admin.products.operating.model.request.ADCreateOperatingRequest;
import com.sd20201.datn.core.admin.products.operating.model.request.ADOperatingRequest;
import com.sd20201.datn.core.admin.products.operating.repository.ADOperatingRepository;
import com.sd20201.datn.core.admin.products.operating.service.ADOperatingService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.OperatingSystem;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ADOperatingServiceImpl implements ADOperatingService
{

    private final ADOperatingRepository adOperatingRepository;

    @Override
    public ResponseObject<?> getAllOperatingSystems(ADOperatingRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");
        return new ResponseObject<>(
                PageableObject.of(adOperatingRepository.getAllOperatingSystems(
                        pageable,
                        request.getKey()
                )),
                HttpStatus.OK,
                "Lấy thành công danh sách hệ điều hành"
        );
    }

    @Override
    public ResponseObject<?> createOperatingSystem(ADCreateOperatingRequest request) {
        OperatingSystem os = new OperatingSystem();
        os.setName(request.getName());
        os.setVersion(request.getVersion());
        os.setDescription(request.getDescription());
        os.setCreatedDate(System.currentTimeMillis());
        os.setStatus(EntityStatus.ACTIVE);

        adOperatingRepository.save(os);

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Thêm mới hệ điều hành thành công",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> updateOperatingSystemStatus(String id) {
        Optional<OperatingSystem> optionalOS = adOperatingRepository.findById(id);
        if (optionalOS.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Hệ điều hành không tồn tại", false, null);
        }

        OperatingSystem os = optionalOS.get();
        os.setStatus(os.getStatus().equals(EntityStatus.ACTIVE) ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
        adOperatingRepository.save(os);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái hệ điều hành thành công", true, null);
    }

    @Override
    public ResponseObject<?> updateOperatingSystem(String id, ADCreateOperatingRequest request) {
        Optional<OperatingSystem> optionalOS = adOperatingRepository.findById(id);
        if (optionalOS.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Hệ điều hành không tồn tại", false, null);
        }

        OperatingSystem os = optionalOS.get();
        os.setName(request.getName());
        os.setVersion(request.getVersion());
        os.setDescription(request.getDescription());

        adOperatingRepository.save(os);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật hệ điều hành thành công", true, null);
    }
}
