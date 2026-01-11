package com.sd20201.datn.core.admin.statistics.service;

import com.sd20201.datn.core.admin.statistics.model.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.io.IOException;
public interface AdStatisticsService {
    AdDashboardOverviewResponse getDashboardOverview();

    // Bảng tốc độ tăng trưởng
    List<AdGrowthStatResponse> getGrowthStatistics();

    // Biểu đồ
    AdRevenueChartResponse getRevenueChart(String type);
    List<AdChartResponse> getOrderStatusChart(String type);
    List<AdChartResponse> getTopProductsChart(String type);

    // Bảng sản phẩm sắp hết
    Page<AdProductResponse> getLowStockProducts(Integer limit, Pageable pageable);
    byte[] exportRevenueToExcel() throws IOException;

}
