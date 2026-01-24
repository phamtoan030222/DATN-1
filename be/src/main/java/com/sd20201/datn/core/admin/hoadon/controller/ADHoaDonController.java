package com.sd20201.datn.core.admin.hoadon.controller;

import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonSearchRequest;
import com.sd20201.datn.core.admin.hoadon.service.ADHoaDonService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_ADMIN_HOA_DON)
@Slf4j
@CrossOrigin(origins = "*")
public class ADHoaDonController {

    public final ADHoaDonService service;

    @GetMapping
    public ResponseEntity<?> getAll(@ModelAttribute ADHoaDonSearchRequest request) {
        return Helper.createResponseEntity(service.getAllHoaDon(request));
    }


}
