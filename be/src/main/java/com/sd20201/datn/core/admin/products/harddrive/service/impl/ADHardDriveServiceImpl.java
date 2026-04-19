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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ADHardDriveServiceImpl implements ADHardDriveService {

    private final ADHardDriveRepository adHardDriveRepository;

    // ==================== GET ALL ====================

    @Override
    public ResponseObject<?> getAllHardDrives(ADHardDriveRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");
        return new ResponseObject<>(
                PageableObject.of(adHardDriveRepository.getAllHardDrives(
                        pageable,
                        request.getKey(),
                        request.getStatus()
                )),
                HttpStatus.OK,
                "Lấy thành công danh sách ổ cứng"
        );
    }

    // ==================== CREATE ====================

    @Override
    public ResponseObject<?> createHardDrive(ADCreateHardDriverRequest request) {
        String name        = request.getName()        == null ? "" : request.getName().trim();
        String brand       = request.getBrand()       == null ? "" : request.getBrand().trim();
        String type        = request.getType()        == null ? "" : request.getType().trim();
        String typeConnect = request.getTypeConnect() == null ? "" : request.getTypeConnect().trim();
        Integer capacity   = request.getCapacity();

        // ── Bước 1: Lấy tất cả ổ cứng cùng hãng ──
        List<HardDrive> sameBrand = adHardDriveRepository.findAllByBrand(brand);

        if (!sameBrand.isEmpty()) {
            // ── Bước 2: Chặn khi trùng TÊN + CẤU HÌNH trong cùng hãng ──
            // Khác cấu hình dù cùng tên → vẫn cho thêm
            boolean isDuplicate = sameBrand.stream()
                    .anyMatch(hd ->
                            hd.getName().trim().equalsIgnoreCase(name)
                                    && hd.getType().trim().equalsIgnoreCase(type)
                                    && hd.getTypeConnect().trim().equalsIgnoreCase(typeConnect)
                                    && hd.getCapacity().equals(capacity)
                    );

            if (isDuplicate) {
                return ResponseObject.errorForward(
                        String.format(
                                "Thêm thất bại! Sản phẩm \"%s\" với cấu hình [%s - %s - %dGB] đã tồn tại trong hãng %s",
                                name, type, typeConnect, capacity, brand
                        ),
                        HttpStatus.CONFLICT
                );
            }
        }

        HardDrive hd = new HardDrive();
        hd.setName(name);
        hd.setBrand(brand);
        hd.setType(type);
        hd.setTypeConnect(typeConnect);
        hd.setCapacity(capacity);
        hd.setReadSpeed(request.getReadSpeed());
        hd.setWriteSpeed(request.getWriteSpeed());
        hd.setCacheMemory(request.getCacheMemory());
        hd.setPhysicalSize(request.getPhysicalSize());
        hd.setDescription(request.getDescription());
        hd.setCreatedDate(System.currentTimeMillis());
        hd.setStatus(EntityStatus.ACTIVE);

        adHardDriveRepository.save(hd);
        return new ResponseObject<>(null, HttpStatus.OK, "Thêm mới ổ cứng thành công");
    }

// ==================== UPDATE ====================

    @Override
    public ResponseObject<?> updateHardDrive(String id, ADCreateHardDriverRequest request) {
        HardDrive current = adHardDriveRepository.findById(id).orElse(null);
        if (current == null) {
            return ResponseObject.errorForward("Ổ cứng không tồn tại", HttpStatus.NOT_FOUND);
        }

        String name        = request.getName()        == null ? "" : request.getName().trim();
        String brand       = request.getBrand()       == null ? "" : request.getBrand().trim();
        String type        = request.getType()        == null ? "" : request.getType().trim();
        String typeConnect = request.getTypeConnect() == null ? "" : request.getTypeConnect().trim();
        Integer capacity   = request.getCapacity();

        // ── Lấy tất cả ổ cứng cùng hãng, loại trừ bản ghi đang sửa ──
        List<HardDrive> sameBrand = adHardDriveRepository.findAllByBrand(brand)
                .stream()
                .filter(hd -> !hd.getId().equals(id))
                .collect(Collectors.toList());

        if (!sameBrand.isEmpty()) {
            // ── Chặn khi trùng TÊN + CẤU HÌNH trong cùng hãng ──
            boolean isDuplicate = sameBrand.stream()
                    .anyMatch(hd ->
                            hd.getName().trim().equalsIgnoreCase(name)
                                    && hd.getType().trim().equalsIgnoreCase(type)
                                    && hd.getTypeConnect().trim().equalsIgnoreCase(typeConnect)
                                    && hd.getCapacity().equals(capacity)
                    );

            if (isDuplicate) {
                return ResponseObject.errorForward(
                        String.format(
                                "Cập nhật thất bại! Sản phẩm \"%s\" với cấu hình [%s - %s - %dGB] đã tồn tại trong hãng %s",
                                name, type, typeConnect, capacity, brand
                        ),
                        HttpStatus.CONFLICT
                );
            }
        }

        current.setName(name);
        current.setBrand(brand);
        current.setType(type);
        current.setTypeConnect(typeConnect);
        current.setCapacity(capacity);
        current.setReadSpeed(request.getReadSpeed());
        current.setWriteSpeed(request.getWriteSpeed());
        current.setCacheMemory(request.getCacheMemory());
        current.setPhysicalSize(request.getPhysicalSize());
        current.setDescription(request.getDescription());

        adHardDriveRepository.save(current);
        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật ổ cứng thành công");
    }
    // ==================== UPDATE STATUS ====================

    @Override
    public ResponseObject<?> updateHardDriveStatus(String id) {
        HardDrive hd = adHardDriveRepository.findById(id).orElse(null);
        if (hd == null) {
            return ResponseObject.errorForward("Không tìm thấy ổ cứng", HttpStatus.NOT_FOUND);
        }

        hd.setStatus(hd.getStatus().equals(EntityStatus.ACTIVE)
                ? EntityStatus.INACTIVE
                : EntityStatus.ACTIVE);

        adHardDriveRepository.save(hd);
        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái thành công");
    }
}