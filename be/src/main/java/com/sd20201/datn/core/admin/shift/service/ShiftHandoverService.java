package com.sd20201.datn.core.admin.shift.service;

import com.sd20201.datn.core.admin.shift.model.request.EndShiftRequest;
import com.sd20201.datn.core.admin.shift.model.request.StartShiftRequest;
import com.sd20201.datn.core.admin.shift.model.response.ShiftHandoverResponse;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.core.common.base.ResponseObject; // Import class này
import org.springframework.data.domain.Page;

public interface ShiftHandoverService {
    ResponseObject<ShiftHandoverResponse> getCurrentShift(String accountId);
    ResponseObject<ShiftHandoverResponse> startShift(StartShiftRequest req);
    ResponseObject<ShiftHandoverResponse> endShift(EndShiftRequest req);
    // Bổ sung vào ShiftHandoverService
    ResponseObject<Page<ShiftHandoverResponse>> getShiftHistory(PageableRequest request);

}
