package com.sd20201.datn.core.admin.shift.controller;

import com.sd20201.datn.core.admin.shift.model.request.ShiftHistoryRequest;
import com.sd20201.datn.core.admin.shift.service.ShiftHandoverService;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_SHIFT_HISTORY) // /api/v1/admin/shift-history
@RequiredArgsConstructor
public class AdminShiftController {

    private final ShiftHandoverService shiftService;

    @GetMapping
    public ResponseEntity<?> getShiftHistory(
            // 👇 Thay bằng class con bạn vừa tạo
            final ShiftHistoryRequest request,
            @RequestParam(required = false) String keyword) {

        // Nhờ tính đa hình (Polymorphism) trong Java,
        // bạn truyền class con vào hàm Service đang yêu cầu class cha thì vẫn hợp lệ 100%!
        return Helper.createResponseEntity(shiftService.getShiftHistory(request, keyword));
    }
}
