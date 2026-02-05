package com.sd20201.datn.core.admin.shift.model.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class CreateScheduleRequest {
    private String staffId;   // Id nhân viên (Account ID)
    private String shiftId;   // Id ca mẫu (Ca Sáng/Chiều)
    private LocalDate workDate;    // Ngày làm việc
    private String note;
}
