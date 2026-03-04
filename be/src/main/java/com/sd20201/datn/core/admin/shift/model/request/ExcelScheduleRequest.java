package com.sd20201.datn.core.admin.shift.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExcelScheduleRequest {
    private String staffCode; // Hoặc Username (Cột A)
    private String workDate;  // Ngày làm việc dạng chuỗi dd/MM/yyyy (Cột B)
    private String shiftName; // Tên ca làm việc (Cột C)
}