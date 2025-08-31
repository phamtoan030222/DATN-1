package com.sd20201.datn.core.admin.discount.voucher.controller;

import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.admin.discount.voucher.service.AdVoucherService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_PRODUCTS_VOUCHER)
@RequiredArgsConstructor
public class AdVoucherController {
    private final AdVoucherService voucherService;

    @GetMapping
    ResponseEntity<?> getAllVouchers(@ModelAttribute AdVoucherRequest request) {
        return Helper.createResponseEntity(voucherService.getVouchers(request));
    }

    @GetMapping("/{id}")
    ResponseEntity<?> getVoucher(@PathVariable String id) {
        return Helper.createResponseEntity(voucherService.getVoucherById(id));
    }

    @PutMapping("/{id}/status")
    ResponseEntity<?> changeStatus(@PathVariable String id) {
        return Helper.createResponseEntity(voucherService.changeStatusVoucher(id));
    }

    @PostMapping()
    public ResponseEntity<?> createVoucher(@Valid @RequestBody AdVoucherCreateUpdateRequest request) {
        return Helper.createResponseEntity(voucherService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVoucher(@PathVariable String id, @RequestBody AdVoucherCreateUpdateRequest request) {
        return Helper.createResponseEntity(voucherService.update(id, request));
    }


    // Xoá 1 voucher
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOne(@PathVariable String id) {
        return Helper.createResponseEntity(voucherService.deleteById(id));
    }

    // Xoá nhiều voucher
    @DeleteMapping
    public ResponseEntity<?> deleteMany(@RequestBody List<String> ids) {
        return Helper.createResponseEntity(voucherService.deleteAllByIds(ids));
    }

    @ControllerAdvice
    public class GlobalExceptionHandler {
        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleException(Exception ex, WebRequest request) {
            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", new Date());
            body.put("status", 500);
            body.put("error", "Internal Server Error");
            body.put("message", ex.getMessage());  // Add chi tiết
            body.put("path", request.getDescription(false).replace("uri=", ""));
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}