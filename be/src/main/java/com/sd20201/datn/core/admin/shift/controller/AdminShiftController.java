package com.sd20201.datn.core.admin.shift.controller;

import com.sd20201.datn.core.admin.shift.service.ShiftHandoverService;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_SHIFT_HISTORY) // /api/v1/admin/shift-history
@RequiredArgsConstructor
public class AdminShiftController {

    private final ShiftHandoverService shiftService;

    @GetMapping
    public ResponseEntity<?> getShiftHistory(final PageableRequest request) {
        return Helper.createResponseEntity(shiftService.getShiftHistory(request));
    }
}
