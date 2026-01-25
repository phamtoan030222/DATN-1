package com.sd20201.datn.core.admin.statistics.service;

import com.sd20201.datn.core.admin.statistics.model.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.io.IOException;
public interface AdStatisticsService {
    AdDashboardOverviewResponse getDashboardOverview();

    // [CẬP NHẬT] Thêm tham số start, end
    AdRevenueChartResponse getRevenueChart(String type, Long start, Long end);

    // [CẬP NHẬT] Thêm tham số start, end
    List<AdChartResponse> getOrderStatusChart(String type, Long start, Long end);

    List<AdChartResponse> getTopProductsChart(String type);

    Page<AdProductResponse> getLowStockProducts(Integer quantity, Pageable pageable);

    List<AdGrowthStatResponse> getGrowthStatistics();

    byte[] exportRevenueToExcel() throws IOException;

}
