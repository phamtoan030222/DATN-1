package com.sd20201.datn.core.admin.statistics.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdDashboardOverviewResponse {
    // Doanh thu
    private Long revenueToday;
    private Long revenueWeek;
    private Long revenueMonth;
    private Long totalRevenue;

    // Đơn hàng
    private Integer totalOrders;
    private Integer successfulOrders;
    private Integer cancelledOrders;
    private Integer pendingOrders;
    private Integer processingOrders;

    // Sản phẩm
    private Integer totalProducts;
    private Integer lowStockProducts;

    // Khách hàng
    private Integer totalCustomers;
    private Integer newCustomersMonth;

    private List<TopItemDTO> topSellingProducts;

    private List<TopItemDTO> topBrands;

    @Data
    @AllArgsConstructor
    public static class TopItemDTO {
        private String name;
        private Long count;
    }
}
