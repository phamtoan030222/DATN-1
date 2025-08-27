package com.sd20201.datn.core.admin.products.harddrive.service.impl;

import com.sd20201.datn.core.admin.products.harddrive.model.request.ADCreateHardDriverRequest;
import com.sd20201.datn.core.admin.products.harddrive.model.request.ADHardDriveRequest;
import com.sd20201.datn.core.admin.products.harddrive.repository.ADHardDriveRepository;
import com.sd20201.datn.core.admin.products.harddrive.service.ADHardDriveService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.HardDrive;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ADHardDriveServiceImpl implements ADHardDriveService {

    private final ADHardDriveRepository adHardDriveRepository;

    @Override
    public ResponseObject<?> getAllHardDrives(ADHardDriveRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");
        return new ResponseObject<>(
                PageableObject.of(adHardDriveRepository.getAllHardDrives(
                        pageable,
                        request.getKey()
                )),
                HttpStatus.OK,
                "Lấy thành công danh sách ổ cứng"
        );
    }

    @Override
    public ResponseObject<?> createHardDrive(ADCreateHardDriverRequest request) {
        HardDrive hd = new HardDrive();
        hd.setName(request.getName());
        hd.setCreatedDate(System.currentTimeMillis());
        hd.setBrand(request.getBrand());
        hd.setType(request.getType());
        hd.setTypeConnect(request.getTypeConnect());
        hd.setCapacity(request.getCapacity());
        hd.setReadSpeed(request.getReadSpeed());
        hd.setWriteSpeed(request.getWriteSpeed());
        hd.setCacheMemory(request.getCacheMemory());
        hd.setPhysicalSize(request.getPhysicalSize());
        hd.setDescription(request.getDescription());
        hd.setStatus(EntityStatus.ACTIVE);

        adHardDriveRepository.save(hd);

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Thêm mới ổ cứng thành công",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> updateHardDriveStatus(String id) {
        Optional<HardDrive> optionalHD = adHardDriveRepository.findById(id);
        if (optionalHD.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Ổ cứng không tồn tại", false, null);
        }

        HardDrive hd = optionalHD.get();
        hd.setStatus(hd.getStatus().equals(EntityStatus.ACTIVE) ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
        adHardDriveRepository.save(hd);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái ổ cứng thành công", true, null);
    }

    @Override
    public ResponseObject<?> updateHardDrive(String id, ADCreateHardDriverRequest request) {
        Optional<HardDrive> optionalHD = adHardDriveRepository.findById(id);
        if (optionalHD.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Ổ cứng không tồn tại", false, null);
        }

        HardDrive hd = optionalHD.get();
        hd.setName(request.getName());
        hd.setBrand(request.getBrand());
        hd.setType(request.getType());
        hd.setTypeConnect(request.getTypeConnect());
        hd.setCapacity(request.getCapacity());
        hd.setReadSpeed(request.getReadSpeed());
        hd.setWriteSpeed(request.getWriteSpeed());
        hd.setCacheMemory(request.getCacheMemory());
        hd.setPhysicalSize(request.getPhysicalSize());
        hd.setDescription(request.getDescription());

        adHardDriveRepository.save(hd);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật ổ cứng thành công", true, null);
    }
}

