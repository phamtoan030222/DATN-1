package com.sd20201.datn.core.admin.statistics.controller;

import com.sd20201.datn.core.admin.statistics.model.response.*;
import com.sd20201.datn.core.admin.statistics.service.AdStatisticsService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_STATISTICS)
@RequiredArgsConstructor
public class AdStatisticsController {
    private final AdStatisticsService adStatisticsService;

    // 1. API Tổng quan
    @GetMapping("/overview")
    public ResponseObject<AdDashboardOverviewResponse> getOverview() {
        return new ResponseObject<>(
                adStatisticsService.getDashboardOverview(),
                HttpStatus.OK,
                "Lấy dữ liệu tổng quan thành công"
        );
    }

    // 2. API Biểu đồ Doanh thu
    // [CẬP NHẬT] Thêm start và end để hỗ trợ lọc tùy chỉnh
    @GetMapping("/chart/revenue")
    public ResponseObject<AdRevenueChartResponse> getRevenueChart(
            @RequestParam(defaultValue = "week") String type,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end) {
        return new ResponseObject<>(
                adStatisticsService.getRevenueChart(type, start, end),
                HttpStatus.OK,
                "Lấy dữ liệu biểu đồ doanh thu thành công"
        );
    }

    // 3. API Biểu đồ Trạng thái đơn hàng
    // [CẬP NHẬT] Thêm start và end
    @GetMapping("/chart/order-status")
    public ResponseObject<List<AdChartResponse>> getOrderStatusChart(
            @RequestParam(defaultValue = "week") String type,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end) {
        return new ResponseObject<>(
                adStatisticsService.getOrderStatusChart(type, start, end),
                HttpStatus.OK,
                "Lấy dữ liệu biểu đồ trạng thái thành công"
        );
    }

    // 4. API Biểu đồ Top Sản phẩm
    @GetMapping("/chart/top-products")
    public ResponseObject<List<AdChartResponse>> getTopProductsChart(
            @RequestParam(defaultValue = "week") String type) {
        return new ResponseObject<>(
                adStatisticsService.getTopProductsChart(type),
                HttpStatus.OK,
                "Lấy dữ liệu top sản phẩm thành công"
        );
    }

    // 5. API Sản phẩm sắp hết hàng
    @GetMapping("/low-stock-product")
    public ResponseObject<Page<AdProductResponse>> getLowStockProducts(
            @RequestParam(defaultValue = "10") Integer quantity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return new ResponseObject<>(
                adStatisticsService.getLowStockProducts(quantity, PageRequest.of(page, size)),
                HttpStatus.OK,
                "Lấy danh sách sản phẩm sắp hết hàng thành công"
        );
    }

    // 6. API Tốc độ tăng trưởng
    @GetMapping("/growth")
    public ResponseObject<List<AdGrowthStatResponse>> getGrowthStats() {
        return new ResponseObject<>(
                adStatisticsService.getGrowthStatistics(),
                HttpStatus.OK,
                "Lấy dữ liệu tăng trưởng thành công"
        );
    }

    // 7. API Xuất Excel
    @GetMapping("/export/revenue")
    public ResponseEntity<byte[]> exportRevenue() {
        try {
            byte[] excelContent = adStatisticsService.exportRevenueToExcel();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BaoCaoDoanhThu.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelContent);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
