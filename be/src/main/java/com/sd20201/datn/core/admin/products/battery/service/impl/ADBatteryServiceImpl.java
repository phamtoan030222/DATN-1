package com.sd20201.datn.core.admin.products.battery.service.impl;

import com.sd20201.datn.core.admin.products.battery.model.request.ADBatteryRequest;
import com.sd20201.datn.core.admin.products.battery.model.request.ADCreateBatteryRequest;
import com.sd20201.datn.core.admin.products.battery.repository.ADBatteryRepository;
import com.sd20201.datn.core.admin.products.battery.service.ADBatteryService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Battery;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ADBatteryServiceImpl implements ADBatteryService {

    private final ADBatteryRepository adBatteryRepository;

    @Override
    public ResponseObject<?> getAllBatteries(ADBatteryRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");
        return new ResponseObject<>(
                PageableObject.of(adBatteryRepository.getAllBatteries(
                        pageable,
                        request.getKey()
                )),
                HttpStatus.OK,
                "Lấy thành công danh sách pin"
        );
    }

    @Override
    public ResponseObject<?> createBattery(ADCreateBatteryRequest request) {
        Battery battery = new Battery();
        battery.setCode(request.getCode());
        battery.setName(request.getName());
        battery.setBrand(request.getBrand());
        battery.setTechnolyCharging(request.getTechnolyCharging());
        battery.setCapacity(request.getCapacity());
        battery.setRemoveBattery(request.getRemoveBattery());
        battery.setTypeBattery(request.getType());
        battery.setCreatedDate(System.currentTimeMillis());
        battery.setStatus(EntityStatus.ACTIVE);

        adBatteryRepository.save(battery);

        return new ResponseObject<>(null, HttpStatus.OK, "Thêm mới pin thành công", true, null);
    }

    @Override
    public ResponseObject<?> updateBatteryStatus(String id) {
        Optional<Battery> optionalBattery = adBatteryRepository.findById(id);
        if (optionalBattery.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Pin không tồn tại", false, null);
        }

        Battery battery = optionalBattery.get();
        battery.setStatus(battery.getStatus().equals(EntityStatus.ACTIVE) ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
        adBatteryRepository.save(battery);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật trạng thái pin thành công", true, null);
    }

    @Override
    public ResponseObject<?> updateBattery(String id, ADCreateBatteryRequest request) {
        Optional<Battery> optionalBattery = adBatteryRepository.findById(id);
        if (optionalBattery.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Pin không tồn tại", false, null);
        }

        Battery battery = optionalBattery.get();
        battery.setCode(request.getCode());
        battery.setName(request.getName());
        battery.setBrand(request.getBrand());
        battery.setTechnolyCharging(request.getTechnolyCharging());
        battery.setCapacity(request.getCapacity());
        battery.setRemoveBattery(request.getRemoveBattery());
        battery.setTypeBattery(request.getType());

        adBatteryRepository.save(battery);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật pin thành công", true, null);
    }
}

