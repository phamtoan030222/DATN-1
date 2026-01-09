package com.sd20201.datn.core.admin.statistics.service.impl;

import com.sd20201.datn.core.admin.statistics.model.response.*;
import com.sd20201.datn.core.admin.statistics.repository.AdCustomerRepository1;
import com.sd20201.datn.core.admin.statistics.repository.AdInvoiceRepository;
import com.sd20201.datn.core.admin.statistics.repository.AdProductDiscountRepository1;
import com.sd20201.datn.core.admin.statistics.service.AdStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;



@Service
@RequiredArgsConstructor
public class AdStatisticsServiceImpl implements AdStatisticsService {

    private final AdInvoiceRepository invoiceRepo;
    private final AdProductDiscountRepository1 productRepo;
    private final AdCustomerRepository1 customerRepo;

    // Helper: Chuyển LocalDateTime -> Long (Mili giây)
    private Long toMilis(LocalDateTime ldt) {
        if (ldt == null) return 0L;
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // Helper: Chuyển Long -> LocalDate
    private LocalDate toDate(Long milis) {
        if (milis == null) return LocalDate.now();
        return Instant.ofEpochMilli(milis).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // --- 1. TỔNG QUAN ---
    @Override
    public AdDashboardOverviewResponse getDashboardOverview() {
        LocalDateTime now = LocalDateTime.now();
        Long nowMs = toMilis(now);

        // Chuyển LocalDateTime sang LocalDate trước khi gọi atStartOfDay()
        Long startDay = toMilis(now.toLocalDate().atStartOfDay());
        Long startWeek = toMilis(now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay());
        Long startMonth = toMilis(now.toLocalDate().withDayOfMonth(1).atStartOfDay());

        // Lấy số liệu khách hàng
        Integer totalCust = 0;
        Integer newCust = 0;
        try {
            totalCust = customerRepo.countAllCustomers();
            newCust = customerRepo.countNewCustomers(startMonth, nowMs);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<AdChartResponse> topProds = productRepo.getTop3BestSelling();
        List<AdDashboardOverviewResponse.TopItemDTO> listProds = new ArrayList<>();
        if (topProds != null) {
            for (AdChartResponse item : topProds) {
                listProds.add(new AdDashboardOverviewResponse.TopItemDTO(item.getName(), item.getValue()));
            }
        }

        // 2. Lấy Top Thương hiệu
        List<AdChartResponse> topBrandsRes = productRepo.getTop3Brands();
        List<AdDashboardOverviewResponse.TopItemDTO> listBrands = new ArrayList<>();
        if (topBrandsRes != null) {
            for (AdChartResponse item : topBrandsRes) {
                listBrands.add(new AdDashboardOverviewResponse.TopItemDTO(item.getName(), item.getValue()));
            }
        }

        return AdDashboardOverviewResponse.builder()
                .totalRevenue(invoiceRepo.sumTotalAmount())
                .revenueToday(invoiceRepo.sumTotalAmountBetween(startDay, nowMs))
                .revenueWeek(invoiceRepo.sumTotalAmountBetween(startWeek, nowMs))
                .revenueMonth(invoiceRepo.sumTotalAmountBetween(startMonth, nowMs))
                .totalOrders(Math.toIntExact(invoiceRepo.countAllInvoices()))
                .successfulOrders(invoiceRepo.countByStatus(0))
                .pendingOrders(invoiceRepo.countByStatus(1))
                .processingOrders(invoiceRepo.countByStatus(3))
                .cancelledOrders(invoiceRepo.countByStatus(2))
                .totalProducts(productRepo.countAllProducts())
                .lowStockProducts(productRepo.countLowStockProducts(10))

                // Data Khách hàng
                .totalCustomers(totalCust)
                .newCustomersMonth(newCust)
                .topSellingProducts(listProds)
                .topBrands(listBrands)
                .build();
    }

    // --- 2. TỐC ĐỘ TĂNG TRƯỞNG ---
    @Override
    public List<AdGrowthStatResponse> getGrowthStatistics() {
        LocalDateTime now = LocalDateTime.now();
        Long nowMs = toMilis(now);

        // FIX LỖI: Logic ngày tháng chuẩn
        Long startToday = toMilis(now.toLocalDate().atStartOfDay());
        Long startYesterday = toMilis(now.minusDays(1).toLocalDate().atStartOfDay());
        Long endYesterday = startToday - 1;

        List<AdGrowthStatResponse> list = new ArrayList<>();

        // Doanh thu ngày
        list.add(calculateGrowth("Doanh thu ngày",
                invoiceRepo.sumTotalAmountBetween(startToday, nowMs),
                invoiceRepo.sumTotalAmountBetween(startYesterday, endYesterday), true));

        // Đơn hàng ngày
        list.add(calculateGrowth("Đơn hàng ngày",
                (long) invoiceRepo.countOrdersBetween(startToday, nowMs),
                (long) invoiceRepo.countOrdersBetween(startYesterday, endYesterday), false));

        return list;
    }

    private AdGrowthStatResponse calculateGrowth(String label, Long current, Long previous, boolean isCurrency) {
        if (current == null) current = 0L;
        if (previous == null) previous = 0L;
        double percent = 0.0;
        if (previous > 0) {
            percent = ((double) (current - previous) / previous) * 100;
        } else if (current > 0) {
            percent = 100.0;
        }
        String trend = percent >= 0 ? "up" : "down";
        String valStr = String.valueOf(current);
        return AdGrowthStatResponse.builder()
                .label(label).value(valStr).percent(String.format("%.2f%%", Math.abs(percent))).trend(trend).build();
    }

    // --- 3. BIỂU ĐỒ DOANH THU ---
    @Override
    public AdRevenueChartResponse getRevenueChart(String type) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start, end, prevStart, prevEnd;
        List<String> labels = new ArrayList<>();
        String groupBy = "day"; // day | hour | month

        switch (type) {
            case "today":
                // 00:00 -> 23:59 hôm nay
                start = now.toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                prevStart = start.minusDays(1);
                prevEnd = end.minusDays(1);
                // Label 0h -> 23h
                for (int i = 0; i < 24; i++) labels.add(i + "h");
                groupBy = "hour";
                break;

            case "week":
                start = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
                end = now.with(DayOfWeek.SUNDAY).toLocalDate().atTime(23, 59, 59);
                prevStart = start.minusWeeks(1);
                prevEnd = end.minusWeeks(1);
                labels = List.of("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN");
                break;

            case "month":
                start = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
                end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).toLocalDate().atTime(23, 59, 59);
                prevStart = start.minusMonths(1);
                prevEnd = end.minusMonths(1);
                for (int i = 1; i <= end.toLocalDate().lengthOfMonth(); i++) labels.add(String.valueOf(i));
                break;

            case "year":
                start = now.withDayOfYear(1).toLocalDate().atStartOfDay();
                end = now.withDayOfYear(now.toLocalDate().lengthOfYear()).toLocalDate().atTime(23, 59, 59);
                prevStart = start.minusYears(1);
                prevEnd = end.minusYears(1);
                for (int i = 1; i <= 12; i++) labels.add("T" + i);
                groupBy = "month";
                break;

            default: // Mặc định là week nếu custom hoặc null
                start = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
                end = now.with(DayOfWeek.SUNDAY).toLocalDate().atTime(23, 59, 59);
                prevStart = start.minusWeeks(1);
                prevEnd = end.minusWeeks(1);
                labels = List.of("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN");
                break;
        }

        List<Long> currentData = processRevenueData(start, end, labels.size(), groupBy);
        List<Long> previousData = processRevenueData(prevStart, prevEnd, labels.size(), groupBy);

        return AdRevenueChartResponse.builder()
                .timeLabels(labels)
                .currentData(currentData)
                .previousData(previousData)
                .build();
    }


    private List<Long> processRevenueData(LocalDateTime start, LocalDateTime end, int steps, String groupBy) {
        List<AdChartResponse> rawData = invoiceRepo.getRawRevenueData(toMilis(start), toMilis(end));
        Map<String, Long> dataMap = new HashMap<>();

        if (rawData != null) {
            for (AdChartResponse item : rawData) {
                try {
                    if (item.getName() == null) continue;
                    long timestamp = Long.parseLong(item.getName());
                    Long amount = (item.getValue() == null) ? 0L : item.getValue();

                    LocalDateTime dt = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
                    String key = "";

                    // Tạo key gom nhóm dữ liệu
                    if ("hour".equals(groupBy)) {
                        key = String.valueOf(dt.getHour()); // Key: 0, 1... 23
                    } else if ("month".equals(groupBy)) {
                        key = String.valueOf(dt.getMonthValue()); // Key: 1... 12
                    } else { // day
                        key = dt.toLocalDate().toString(); // Key: 2026-01-09
                    }

                    dataMap.put(key, dataMap.getOrDefault(key, 0L) + amount);
                } catch (Exception e) { continue; }
            }
        }

        List<Long> result = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            String keyCheck = "";
            if ("hour".equals(groupBy)) {
                keyCheck = String.valueOf(i); // Check key 0..23
            } else if ("month".equals(groupBy)) {
                keyCheck = String.valueOf(i + 1); // Check key 1..12
            } else { // day
                keyCheck = start.toLocalDate().plusDays(i).toString();
            }
            result.add(dataMap.getOrDefault(keyCheck, 0L));
        }
        return result;
    }
    // --- 4. CHART KHÁC & LOW STOCK ---
    @Override
    public List<AdChartResponse> getOrderStatusChart(String type) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = "month".equals(type) ? end.withDayOfMonth(1).toLocalDate().atStartOfDay() : end.minusDays(6).toLocalDate().atStartOfDay();
        return invoiceRepo.getOrderStatusStats(toMilis(start), toMilis(end));
    }

    @Override
    public List<AdChartResponse> getTopProductsChart(String type) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = "month".equals(type) ? end.withDayOfMonth(1).toLocalDate().atStartOfDay() : end.minusDays(6).toLocalDate().atStartOfDay();
        return productRepo.getTopSellingProductsChart(toMilis(start), toMilis(end), PageRequest.of(0, 10));
    }

    @Override
    public Page<AdProductResponse> getLowStockProducts(Integer limit, Pageable pageable) {
        return productRepo.getLowStockProducts(limit, pageable);
    }
}
