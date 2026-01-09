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

import java.util.List;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_STATISTICS)
@RequiredArgsConstructor
public class AdStatisticsController {
    private final AdStatisticsService adStatisticsService;

    // 1. API Tổng quan (3 Card trên cùng)
    @GetMapping("/overview")
    public ResponseObject<AdDashboardOverviewResponse> getOverview() {
        return new ResponseObject<>(
                adStatisticsService.getDashboardOverview(),
                HttpStatus.OK,
                "Lấy dữ liệu tổng quan thành công"
        );
    }

    // 2. API Bảng Tốc độ tăng trưởng
    @GetMapping("/growth")
    public ResponseObject<List<AdGrowthStatResponse>> getGrowthStats() {
        return new ResponseObject<>(
                adStatisticsService.getGrowthStatistics(),
                HttpStatus.OK,
                "Lấy dữ liệu tăng trưởng thành công"
        );
    }

    // 3. API Sản phẩm sắp hết hàng (Params: quantity, page, size)
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

    // 4. API Biểu đồ Doanh thu (Line Chart)
    @GetMapping("/chart/revenue")
    public ResponseObject<AdRevenueChartResponse> getRevenueChart(
            @RequestParam(defaultValue = "week") String type) {

        return new ResponseObject<>(
                adStatisticsService.getRevenueChart(type),
                HttpStatus.OK,
                "Lấy dữ liệu biểu đồ doanh thu thành công"
        );
    }

    // 5. API Biểu đồ Trạng thái đơn hàng (Pie Chart)
    @GetMapping("/chart/order-status")
    public ResponseObject<List<AdChartResponse>> getOrderStatusChart(
            @RequestParam(defaultValue = "week") String type) {

        return new ResponseObject<>(
                adStatisticsService.getOrderStatusChart(type),
                HttpStatus.OK,
                "Lấy dữ liệu biểu đồ trạng thái thành công"
        );
    }

    // 6. API Biểu đồ Top Sản phẩm bán chạy (Bar Chart)
    @GetMapping("/chart/top-products")
    public ResponseObject<List<AdChartResponse>> getTopProductsChart(
            @RequestParam(defaultValue = "week") String type) {

        return new ResponseObject<>(
                adStatisticsService.getTopProductsChart(type),
                HttpStatus.OK,
                "Lấy dữ liệu top sản phẩm thành công"
        );
    }

}
