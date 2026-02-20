package com.sd20201.datn.core.admin.staff.controller;

import com.sd20201.datn.core.admin.staff.model.request.ADCreateStaff;
import com.sd20201.datn.core.admin.staff.model.request.ADStaffRequest;
import com.sd20201.datn.core.admin.staff.service.ADStaffService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_STAFF)
public class ADStaffController {
    private final ADStaffService adStaffService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('QUAN_LY', 'NHAN_VIEN')")
    public ResponseEntity<?> getAllStaff(@ModelAttribute ADStaffRequest request) {
        return Helper.createResponseEntity(adStaffService.getAllStaff(request));
    }

    // GIỮ NGUYÊN "/add" để khớp với FE hiện tại
    @PostMapping("/add")
    public ResponseEntity<?> createStaff(@RequestBody @Valid ADCreateStaff request) {
        return Helper.createResponseEntity(adStaffService.createStaff(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable String id, @RequestBody @Valid ADCreateStaff request) {
        return Helper.createResponseEntity(adStaffService.updateStaff(id, request));
    }

    // GIỮ NGUYÊN "/{id}/status"
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateBrandStatus(@PathVariable String id) {
        return Helper.createResponseEntity(adStaffService.changeStatusStaff(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStaffDetail(@PathVariable String id) {
        return Helper.createResponseEntity(adStaffService.getStaffById(id));
    }
}
