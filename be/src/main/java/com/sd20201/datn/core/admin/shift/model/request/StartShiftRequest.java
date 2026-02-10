package com.sd20201.datn.core.admin.shift.model.request;

import com.fasterxml.jackson.annotation.JsonFormat; // <--- Quan trọng
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class StartShiftRequest {

    private String accountId;
    private BigDecimal initialCash;
    private String note;

    // 👇 Thêm trường này để nhận tên ca từ Frontend
    private String name;

    // 👇 QUAN TRỌNG: Phải có dòng @JsonFormat này mới khớp với hàm formatDateForServer ở FE
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String workScheduleId;
}