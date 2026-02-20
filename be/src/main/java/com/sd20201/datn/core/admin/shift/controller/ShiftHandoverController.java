package com.sd20201.datn.core.admin.shift.controller;

import com.sd20201.datn.core.admin.shift.model.request.EndShiftRequest;
import com.sd20201.datn.core.admin.shift.model.request.StartShiftRequest;
import com.sd20201.datn.core.admin.shift.service.ShiftHandoverService;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(MappingConstants.API_STAFF_PREFIX_SHIFTS)
@RequiredArgsConstructor
@CrossOrigin("*")
public class ShiftHandoverController {

    private final ShiftHandoverService shiftService;

    @GetMapping("/current")
    public ResponseEntity<?> getCurrent(@RequestParam String accountId) {
        // Log ra để kiểm tra xem FE có gửi đúng ID không
        System.out.println("Check current shift for Account ID: " + accountId);

        var res = shiftService.getCurrentShift(accountId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // 2. Mở ca
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody @Valid StartShiftRequest req) {
        var res = shiftService.startShift(req);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // 3. Kết ca
    @PostMapping("/end")
    public ResponseEntity<?> end(@RequestBody @Valid EndShiftRequest req) {
        var res = shiftService.endShift(req);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/last-closed")
    public ResponseEntity<?> getLastClosedShift() {
        var res = shiftService.getLastClosedShift();
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@ModelAttribute PageableRequest request, @RequestParam(required = false) String keyword) {
        var res = shiftService.getShiftHistory(request, keyword);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}