package com.sd20201.datn.core.admin.products.ram.service.impl;

import com.sd20201.datn.core.admin.products.ram.model.request.ADCreateRam;
import com.sd20201.datn.core.admin.products.ram.model.request.ADRamRequest;
import com.sd20201.datn.core.admin.products.ram.repository.ADRamRepository;
import com.sd20201.datn.core.admin.products.ram.service.ADRamService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.RAM;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ADRamServiceImpl implements ADRamService {

    private final ADRamRepository adRamRepository;

    @Override
    public ResponseObject<?> getAllRam(ADRamRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");
        return new ResponseObject<>(
                PageableObject.of(adRamRepository.getAllRam(pageable, request.getKey(), request.getStatus())),
                HttpStatus.OK,
                "Lấy thành công danh sách RAM"
        );
    }

    @Override
    public ResponseObject<?> createRam(ADCreateRam request) {
        // 1. Chuẩn hóa các trường String
        String brand = request.getBrand() == null ? "" : request.getBrand().trim();
        String type = request.getType() == null ? "" : request.getType().trim();

        // 2. Lấy các trường Integer (Dữ liệu mặc định của bạn)
        Integer capacity = request.getCapacity();
        Integer busSpeed = request.getBusSpeed();

        // 3. Check trùng dựa trên tổ hợp cấu hình
        Optional<RAM> existingRam = adRamRepository.checkDuplicate(brand, capacity, type, busSpeed);
        if (existingRam.isPresent()) {
            return ResponseObject.errorForward("Thêm thất bại! Cấu hình RAM này đã tồn tại.", HttpStatus.CONFLICT);
        }

        RAM ram = new RAM();
        ram.setName(request.getName());
        ram.setBrand(brand);
        ram.setType(type);
        ram.setCapacity(capacity);
        ram.setBusSpeed(busSpeed);
        ram.setSlotConFig(request.getSlotConFig());
        ram.setMaxSupported(request.getMaxSupported());
        ram.setDescription(request.getDescription());

        ram.setStatus(EntityStatus.ACTIVE);
        ram.setCreatedDate(System.currentTimeMillis());

        adRamRepository.save(ram);
        return new ResponseObject<>(null, HttpStatus.OK, "Thêm thành công RAM", true, null);
    }

    @Override
    public ResponseObject<?> updateRam(String id, ADCreateRam request) {
        Optional<RAM> optionalRam = adRamRepository.findById(id);
        if (optionalRam.isEmpty()) {
            return ResponseObject.errorForward("RAM không tồn tại", HttpStatus.NOT_FOUND);
        }

        String brand = request.getBrand() == null ? "" : request.getBrand().trim();
        String type = request.getType() == null ? "" : request.getType().trim();
        Integer capacity = request.getCapacity();
        Integer busSpeed = request.getBusSpeed();

        // Check trùng nhưng loại trừ ID hiện tại
        Optional<RAM> existingRam = adRamRepository.checkDuplicate(brand, capacity, type, busSpeed);
        if (existingRam.isPresent() && !existingRam.get().getId().equals(id)) {
            return ResponseObject.errorForward("Cập nhật thất bại! Cấu hình RAM này đã tồn tại.", HttpStatus.CONFLICT);
        }

        RAM ram = optionalRam.get();
        ram.setName(request.getName());
        ram.setBrand(brand);
        ram.setType(type);
        ram.setCapacity(capacity);
        ram.setBusSpeed(busSpeed);
        ram.setSlotConFig(request.getSlotConFig());
        ram.setMaxSupported(request.getMaxSupported());
        ram.setDescription(request.getDescription());

        adRamRepository.save(ram);
        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật RAM thành công", true, null);
    }
    @Override
    public ResponseObject<?> updateRamStatus(String id) {
        Optional<RAM> optionalRam = adRamRepository.findById(id);
        if (optionalRam.isEmpty()) {
            return ResponseObject.errorForward("RAM không tồn tại", HttpStatus.NOT_FOUND);
        }

        RAM ram = optionalRam.get();
        ram.setStatus(ram.getStatus().equals(EntityStatus.ACTIVE) ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
        adRamRepository.save(ram);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái RAM thành công", true, null);
    }
}