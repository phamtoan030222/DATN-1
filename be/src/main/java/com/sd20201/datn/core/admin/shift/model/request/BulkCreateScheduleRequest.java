package com.sd20201.datn.core.admin.shift.model.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class BulkCreateScheduleRequest {
    private String staffId;            // Nhân viên được chọn
    private List<String> shiftIds;     // Danh sách các ca được chọn
    private LocalDate startDate;       // Từ ngày
    private LocalDate endDate;         // Đến ngày
    private List<Integer> daysOfWeek;  // Các ngày lặp lại (1=Thứ 2, ..., 7=Chủ Nhật)
    private Boolean overwrite;
}