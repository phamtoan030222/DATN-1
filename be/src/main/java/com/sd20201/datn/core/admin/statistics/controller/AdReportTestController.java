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
    public String testSendMail() {
        try {
            adReportScheduledService.runAutoReporting();
            return "Đã kích hoạt gửi báo cáo thử nghiệm! Hãy kiểm tra hòm thư của Quản lý.";
        } catch (Exception e) {
            return "Lỗi khi gửi mail: " + e.getMessage();
        }
    }
}