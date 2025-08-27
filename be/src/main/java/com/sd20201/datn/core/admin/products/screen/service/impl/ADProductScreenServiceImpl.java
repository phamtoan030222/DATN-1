package com.sd20201.datn.core.admin.products.screen.service.impl;

import com.sd20201.datn.core.admin.products.screen.model.request.ADProductScreenCreateUpdateRequest;
import com.sd20201.datn.core.admin.products.screen.model.request.ADProductScreenRequest;
import com.sd20201.datn.core.admin.products.screen.repository.ADProductScreenRepository;
import com.sd20201.datn.core.admin.products.screen.repository.ADProductScreenResolutionRepository;
import com.sd20201.datn.core.admin.products.screen.service.ADProductScreenService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Screen;
import com.sd20201.datn.entity.ScreenResolution;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.TypeScreenResolution;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ADProductScreenServiceImpl implements ADProductScreenService {

    private final ADProductScreenRepository screenRepository;

    private final ADProductScreenResolutionRepository screenResolutionRepository;

    @Override
    public ResponseObject<?> getScreens(ADProductScreenRequest request) {
        return ResponseObject.successForward(
                PageableObject.of(screenRepository.getScreens(Helper.createPageable(request), request)),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getDetail(String id) {
        return screenRepository.getScreenById(id).map(gpu -> ResponseObject.successForward(gpu, "Get detail screen successfully"))
                .orElse(ResponseObject.errorForward("Get detail fail!!!", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> changeStatus(String id) {
        return screenRepository.findById(id)
                .map(screen -> {
                    screen.setStatus(screen.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
                    screenRepository.save(screen);
                    return ResponseObject.successForward(null, "Update status screen successfully");
                })
                .orElse(ResponseObject.errorForward("Update status screen failure!!! Screen not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> modify(ADProductScreenCreateUpdateRequest request) {
        return request.getId() == null || request.getId().isEmpty()  ? create(request) : update(request);
    }

    private ResponseObject<?> update(ADProductScreenCreateUpdateRequest request) {
        Optional<Screen> optionalScreen = screenRepository.findById(request.getId());

        if(optionalScreen.isEmpty()) return ResponseObject.errorForward("Update fail!!! Screen not found", HttpStatus.NOT_FOUND);

        Screen screen = optionalScreen.get();

        screen.setCode(request.getCode());
        screen.setName(request.getName());
        screen.setPhysicalSize(request.getPhysicalSize());
        screen.setPanelType(request.getPanelType());
        screen.setResolution(TypeScreenResolution.getByCode(request.getResolution()));
        screen.setTechnology(request.getTechnology());

        return ResponseObject.successForward(screenRepository.save(screen), "Screen updated successfully");
    }

    private ResponseObject<?> create(ADProductScreenCreateUpdateRequest request) {
        Screen screen = new Screen();

        Optional<Screen> screenOptional = screenRepository.findByCode(request.getCode());
        if(screenOptional.isPresent()) return ResponseObject.errorForward("Update fail!!! Duplicate code", HttpStatus.CONFLICT);

        screen.setCode(request.getCode());
        screen.setName(request.getName());
        screen.setPhysicalSize(request.getPhysicalSize());
        screen.setPanelType(request.getPanelType());
        screen.setResolution(TypeScreenResolution.getByCode(request.getResolution()));
        screen.setTechnology(request.getTechnology());

        return ResponseObject.successForward(screenRepository.save(screen), "Screen created successfully");
    }
}
