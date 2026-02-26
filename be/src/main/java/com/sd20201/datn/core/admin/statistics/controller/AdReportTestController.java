package com.sd20201.datn.core.admin.statistics.controller;

import com.sd20201.datn.core.admin.statistics.service.impl.AdReportScheduledService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/test-report")
@RequiredArgsConstructor
public class AdReportTestController {

    private final AdReportScheduledService adReportScheduledService;

    // 1. Test gửi báo cáo NGÀY
    @GetMapping("/send-now")
    public String testSendDailyMail() {
        try {
            // "Ngay" là tiêu đề in ra PDF, "ngày" là từ khóa để lọc dữ liệu
            adReportScheduledService.executeReporting("Ngay", "ngày");
            return "Đã kích hoạt gửi báo cáo NGÀY thử nghiệm! Hãy kiểm tra hòm thư.";
        } catch (Exception e) {
            return "Lỗi khi gửi mail ngày: " + e.getMessage();
        }
    }

    // 2. Test gửi báo cáo TUẦN
    @GetMapping("/send-week-now")
    public String testSendWeeklyMail() {
        try {
            adReportScheduledService.executeReporting("Tuan", "tuần");
            return "Đã kích hoạt gửi báo cáo TUẦN thử nghiệm! Hãy kiểm tra hòm thư.";
        } catch (Exception e) {
            return "Lỗi khi gửi mail tuần: " + e.getMessage();
        }
    }

    // 3. Test gửi báo cáo THÁNG
    @GetMapping("/send-month-now")
    public String testSendMonthlyMail() {
        try {
            adReportScheduledService.executeReporting("Thang", "tháng");
            return "Đã kích hoạt gửi báo cáo THÁNG thử nghiệm! Hãy kiểm tra hòm thư.";
        } catch (Exception e) {
            return "Lỗi khi gửi mail tháng: " + e.getMessage();
        }
    }

    // 4. Test gửi báo cáo NĂM
    @GetMapping("/send-year-now")
    public String testSendYearlyMail() {
        try {
            adReportScheduledService.executeReporting("Nam", "năm");
            return "Đã kích hoạt gửi báo cáo NĂM thử nghiệm! Hãy kiểm tra hòm thư.";
        } catch (Exception e) {
            return "Lỗi khi gửi mail năm: " + e.getMessage();
        }
    }
}