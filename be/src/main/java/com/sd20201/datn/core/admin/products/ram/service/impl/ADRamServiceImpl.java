package com.sd20201.datn.core.admin.products.ram.service.impl;

import com.sd20201.datn.core.admin.products.ram.model.request.ADCreateRam;
import com.sd20201.datn.core.admin.products.ram.model.request.ADRamRequest;
import com.sd20201.datn.core.admin.products.ram.repository.ADRamRepository;
import com.sd20201.datn.core.admin.products.ram.service.ADRamService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Product;
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
                PageableObject.of(adRamRepository.getAllRam(pageable,request.getName())),
                        HttpStatus.OK,
                        "Lấy thành công danhh sách ram"
        );
    }

    @Override
    public ResponseObject<?> createRam(ADCreateRam request) {
        RAM  ram = new RAM();
        ram.setName(request.getName());
        ram.setCreatedDate(System.currentTimeMillis());
        ram.setBrand(request.getBrand());
        ram.setDescription(request.getDescription());
        ram.setStatus(EntityStatus.ACTIVE);
        ram.setType(request.getType());
        ram.setCapacity(request.getCapacity());
        ram.setSlotConFig(request.getSlotConFig());
        ram.setMaxSupported(request.getMaxSupported());
        ram.setBusSpeed(request.getBusSpeed());

        adRamRepository.save(ram);

        return new  ResponseObject<>(
                null, HttpStatus.OK,
                "Thêm thành công ram",
                true, null
        );
    }

    @Override
    public ResponseObject<?> updateRamStatus(String id) {
        Optional<RAM> optionalRam = adRamRepository.findById(id);
        if (optionalRam.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "RAM không tồn tại", false, null);
        }

        RAM ram = optionalRam.get();
        ram.setStatus(ram.getStatus().equals(EntityStatus.ACTIVE) ? EntityStatus.INACTIVE : EntityStatus.ACTIVE); // toggle 0/1
        adRamRepository.save(ram);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái RAM thành công", true, null);
    }

    @Override
    public ResponseObject<?> updateRam(String id, ADCreateRam request) {
        Optional<RAM> optionalRam = adRamRepository.findById(id);
        if (optionalRam.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "RAM không tồn tại", false, null);
        }

        RAM ram = optionalRam.get();
        ram.setName(request.getName());
        ram.setBrand(request.getBrand());
        ram.setType(request.getType());
        ram.setCapacity(request.getCapacity());
        ram.setBusSpeed(request.getBusSpeed());
        ram.setSlotConFig(request.getSlotConFig());
        ram.setMaxSupported(request.getMaxSupported());
        ram.setDescription(request.getDescription());

        adRamRepository.save(ram);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật RAM thành công", true, null);
    }
}
