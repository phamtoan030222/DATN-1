package com.sd20201.datn.core.admin.hoadon.controller;

import com.sd20201.datn.core.admin.hoadon.model.request.*;
import com.sd20201.datn.core.admin.hoadon.service.ADHoaDonService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;

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

    @GetMapping("/all")
    public ResponseEntity<?> getHDCT(@ModelAttribute ADHoaDonDetailRequest request) {
        return Helper.createResponseEntity(service.getAllHoaDonCT(request));
    }

    @PutMapping("/change-status")
    public ResponseEntity<?> changeStatus(@RequestBody ADChangeStatusRequest adChangeStatusRequest) {
        return Helper.createResponseEntity(service.capNhatTrangThaiHoaDon(adChangeStatusRequest));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamHoaDon() {
        SseEmitter emitter = new SseEmitter(600000L);
        emitter.onCompletion(() -> System.out.println("SSE Hoàn thành kết nối"));
        emitter.onTimeout(() -> System.out.println("SSE Hết hạn kết nối"));
        return emitter;
    }

    @PutMapping("/doi-imei")
    public ResponseEntity<?> doiImei(@RequestBody ADDoiImeiRequest request) {
        return Helper.createResponseEntity(service.doiImei(request));
    }

    @PutMapping("/cap-nhat-khach-hang")
    public ResponseEntity<?> capNhatKhachHang(
            @RequestBody ADCapNhatKhachHangRequest request) {
        return Helper.createResponseEntity(
                service.capNhatThongTinKhachHang(request)
        );
    }
}
