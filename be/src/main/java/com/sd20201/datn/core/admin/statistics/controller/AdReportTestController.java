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

    @GetMapping("/send-now")
    public String testSendDailyMail() {
        adReportScheduledService.executeReporting("Ngay");
        return "Đã kích hoạt gửi báo cáo NGÀY thử nghiệm! Hãy kiểm tra hòm thư.";
    }

    @GetMapping("/send-week-now")
    public String testSendWeeklyMail() {
        adReportScheduledService.executeReporting("Tuan");
        return "Đã kích hoạt gửi báo cáo TUẦN thử nghiệm! Hãy kiểm tra hòm thư.";
    }

    @GetMapping("/send-month-now")
    public String testSendMonthlyMail() {
        adReportScheduledService.executeReporting("Thang");
        return "Đã kích hoạt gửi báo cáo THÁNG thử nghiệm! Hãy kiểm tra hòm thư.";
    }

    @GetMapping("/send-quarter-now")
    public String testSendQuarterMail() {
        adReportScheduledService.executeReporting("Quy");
        return "Đã kích hoạt gửi báo cáo QUÝ thử nghiệm! Hãy kiểm tra hòm thư.";
    }

    @GetMapping("/send-year-now")
    public String testSendYearlyMail() {
        adReportScheduledService.executeReporting("Nam");
        return "Đã kích hoạt gửi báo cáo NĂM thử nghiệm! Hãy kiểm tra hòm thư.";
    }
}