package com.sd20201.datn.core.admin.shift.controller;

import com.sd20201.datn.core.admin.shift.model.request.BulkCreateScheduleRequest;
import com.sd20201.datn.core.admin.shift.model.request.CreateScheduleRequest;
import com.sd20201.datn.core.admin.shift.service.WorkScheduleService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Arrays;
import java.util.List;
import com.sd20201.datn.utils.ExcelHelper;

import java.time.LocalDate;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_SCHEDULES) // /api/v1/admin/schedules
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleService scheduleService;

    // API hiển thị lịch lên Calendar
    @GetMapping
    @PreAuthorize("hasAnyAuthority('QUAN_LY', 'NHAN_VIEN')")
    public ResponseEntity<?> getSchedules(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        return Helper.createResponseEntity(scheduleService.getSchedules(fromDate, toDate));
    }

    // API xếp lịch mới
    @PostMapping
    public ResponseEntity<?> createSchedule(@RequestBody CreateScheduleRequest req) {
        return Helper.createResponseEntity(scheduleService.createSchedule(req));
    }

    // Trong WorkScheduleController.java

    // 👇 THÊM HÀM NÀY ĐỂ SỬA LỖI 404 KHI XÓA
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable String id) {
        scheduleService.deleteSchedule(id);
        return Helper.createResponseEntity(new ResponseObject<>(null, HttpStatus.OK, "Đã hủy lịch thành công"));
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> createBulkSchedule(@RequestBody BulkCreateScheduleRequest req) {
        return Helper.createResponseEntity(scheduleService.createBulkSchedule(req));
    }

    // 👇 API MỚI: IMPORT EXCEL 👇
    @PostMapping("/import")
    public ResponseEntity<?> importSchedule(@RequestParam("file") MultipartFile file) {
        // Gọi thẳng vào hàm importExcelSchedule mà chúng ta đã viết ở Service
        return Helper.createResponseEntity((ResponseObject<?>) scheduleService.importExcelSchedule(file));
    }

    // 👇 API MỚI: TẢI FILE EXCEL MẪU 👇
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        // 1. Chỉ để lại 3 tiêu đề cột
        List<String> headers = Arrays.asList("Mã nhân viên", "Ngày làm (dd/MM/yyyy)", "Tên ca");

        // 2. Dữ liệu mẫu cũng chỉ gồm 3 trường
        List<List<Object>> data = Arrays.asList(
                Arrays.asList("NV001", "25/12/2026", "ca sáng"),
                Arrays.asList("dungchoctao2k1", "26/12/2026", "Ca Tối 1")
        );

        byte[] excelContent = ExcelHelper.createExcelStream("Mau_Xep_Lich", headers, data);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        httpHeaders.setContentDispositionFormData("attachment", "Template_XepLich.xlsx");

        return new ResponseEntity<>(excelContent, httpHeaders, HttpStatus.OK);
    }
}