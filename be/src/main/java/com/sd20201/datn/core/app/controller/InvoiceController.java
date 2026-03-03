package com.sd20201.datn.core.app.controller;

import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonDetailRequest;
import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonSearchRequest;
import com.sd20201.datn.core.admin.hoadon.service.ADHoaDonService;
import com.sd20201.datn.utils.Helper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/app/hoa-don")
public class InvoiceController {

    @Autowired
    private  ADHoaDonService service;

    @GetMapping("get-all")
    public ResponseEntity<?> getAll(@ModelAttribute ADHoaDonSearchRequest request) {
        return Helper.createResponseEntity(service.getAllHoaDon(request));
    }

    @GetMapping("/hdct")
    public ResponseEntity<?> getHDCT(@ModelAttribute ADHoaDonDetailRequest request) {
        return Helper.createResponseEntity(service.getAllHoaDonCT(request));
    }

    

}
