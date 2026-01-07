package com.sd20201.datn.core.admin.staff.controller;

import com.sd20201.datn.core.admin.staff.model.request.ADCreateStaff;
import com.sd20201.datn.core.admin.staff.model.request.ADStaffRequest;
import com.sd20201.datn.core.admin.staff.service.ADStaffService;
import com.sd20201.datn.core.admin.staff.service.impl.StaffService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import jakarta.mail.MessagingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_STAFF)
public class ADStaffController {

    private final ADStaffService adStaffService;

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<?> getAllStaff(@ModelAttribute ADStaffRequest request) {
        return Helper.createResponseEntity(adStaffService.getAllStaff(request));
    }

    @PostMapping("/add")
    public ResponseEntity<?> createStaff(@RequestBody ADCreateStaff request) throws MessagingException {
        return Helper.createResponseEntity(staffService.createStaff(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable String id, @RequestBody ADCreateStaff request) throws MessagingException {
        return Helper.createResponseEntity(staffService.updateStaff(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateBrandStatus(@PathVariable String id) {
        return Helper.createResponseEntity(adStaffService.changeStatusStaff(id));
    }


}
