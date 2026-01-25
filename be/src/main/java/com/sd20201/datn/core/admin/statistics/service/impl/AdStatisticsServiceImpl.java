package com.sd20201.datn.core.admin.statistics.service.impl;

import com.sd20201.datn.core.admin.statistics.model.response.*;
import com.sd20201.datn.core.admin.statistics.repository.AdStatisticsRepository;
import com.sd20201.datn.core.admin.statistics.service.AdStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class AdStatisticsServiceImpl implements AdStatisticsService {

    private final AdStatisticsRepository statisticsRepo;


    // --- 1. TỔNG QUAN (4 CARDS) ---
    @Override
    public AdDashboardOverviewResponse getDashboardOverview() {
        LocalDateTime now = LocalDateTime.now();

        // Lấy danh sách Top 5 sản phẩm bán chạy (có ảnh và giá)
        List<Object[]> rawTopProducts = statisticsRepo.getTopSellingProductsWithDetail();
        List<AdDashboardOverviewResponse.TopItemDTO> topProducts = rawTopProducts.stream()
                .map(row -> AdDashboardOverviewResponse.TopItemDTO.builder()
                        .name((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .price(((Number) row[2]).doubleValue())
                        .image((String) row[3])
                        .build())
                .collect(Collectors.toList());

        return AdDashboardOverviewResponse.builder()
                .today(getStatsForPeriod(now.toLocalDate().atStartOfDay(), now))
                .week(getStatsForPeriod(now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay(), now))
                .month(getStatsForPeriod(now.withDayOfMonth(1).toLocalDate().atStartOfDay(), now))
                .year(getStatsForPeriod(now.withDayOfYear(1).toLocalDate().atStartOfDay(), now))
                .topSellingProducts(topProducts)
                .build();
    }

    private AdDashboardOverviewResponse.StatisticCard getStatsForPeriod(LocalDateTime start, LocalDateTime end) {
        // Gọi query duy nhất để lấy toàn bộ chỉ số của 1 khoảng thời gian
        Map<String, Object> data = statisticsRepo.getStatsByPeriod(toMs(start), toMs(end));

        return AdDashboardOverviewResponse.StatisticCard.builder()
                .revenue(toLong(data.get("revenue")))
                .soldProducts(toInt(data.get("soldProducts")))
                .totalOrders(toInt(data.get("totalOrders")))
                .completed(toInt(data.get("completed")))
                .cancelled(toInt(data.get("cancelled")))
                .processing(toInt(data.get("processing")))
                .growthRate("+0%") // Logic này có thể tính thêm nếu bạn cần so sánh kỳ trước
                .build();
    }

    // --- 2. BIỂU ĐỒ DOANH THU (LINE CHART) ---
    @Override
    public AdRevenueChartResponse getRevenueChart(String type, Long startCustom, Long endCustom) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start, end, prevStart, prevEnd;
        List<String> labels = new ArrayList<>();
        String groupBy; // "hour", "day", "month"

        switch (type) {
            case "today" -> {
                start = now.toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                // Kỳ trước: Hôm qua
                prevStart = start.minusDays(1);
                prevEnd = end.minusDays(1);
                for (int i = 0; i < 24; i++) labels.add(i + "h");
                groupBy = "hour";
            }
            case "month" -> {
                LocalDate firstDay = now.toLocalDate().withDayOfMonth(1);
                start = firstDay.atStartOfDay();
                LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
                end = lastDay.atTime(23, 59, 59);
                // Kỳ trước: Tháng trước
                LocalDate prevFirstDay = firstDay.minusMonths(1);
                prevStart = prevFirstDay.atStartOfDay();
                LocalDate prevLastDay = prevFirstDay.withDayOfMonth(prevFirstDay.lengthOfMonth());
                prevEnd = prevLastDay.atTime(23, 59, 59);

                for (int i = 1; i <= firstDay.lengthOfMonth(); i++) labels.add(String.valueOf(i));
                groupBy = "day";
            }
            case "year" -> {
                start = now.withDayOfYear(1).toLocalDate().atStartOfDay();
                end = now.withDayOfYear(now.toLocalDate().lengthOfYear()).toLocalDate().atTime(23, 59, 59);
                prevStart = start.minusYears(1);
                prevEnd = end.minusYears(1);
                for (int i = 1; i <= 12; i++) labels.add("T" + i);
                groupBy = "month";
            }
            case "custom" -> {
                // Xử lý logic tùy chỉnh
                if (startCustom == null || endCustom == null) {
                    // Fallback về tuần này nếu thiếu tham số
                    start = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
                    end = now.with(DayOfWeek.SUNDAY).toLocalDate().atTime(23, 59, 59);
                } else {
                    start = Instant.ofEpochMilli(startCustom).atZone(ZoneId.systemDefault()).toLocalDateTime();
                    end = Instant.ofEpochMilli(endCustom).atZone(ZoneId.systemDefault()).toLocalDateTime();
                }

                // Tính khoảng cách ngày để xác định kỳ trước tương ứng
                long daysDiff = ChronoUnit.DAYS.between(start, end);
                prevStart = start.minusDays(daysDiff);
                prevEnd = end.minusDays(daysDiff);

                // Tự động sinh Labels dựa trên độ dài khoảng thời gian
                if (daysDiff <= 1) {
                    // Chọn 1 ngày -> Hiện theo giờ
                    for (int i = 0; i < 24; i++) labels.add(i + "h");
                    groupBy = "hour";
                } else if (daysDiff <= 31) {
                    // Dưới 1 tháng -> Hiện theo ngày (1, 2, 3...)
                    for (int i = 0; i <= daysDiff; i++) {
                        labels.add(String.valueOf(start.plusDays(i).getDayOfMonth()));
                    }
                    groupBy = "day";
                } else {
                    // Trên 1 tháng -> Hiện theo tháng (T1, T2...)
                    groupBy = "month";
                    long months = ChronoUnit.MONTHS.between(start, end);
                    for (int i = 0; i <= months; i++) {
                        labels.add("T" + start.plusMonths(i).getMonthValue());
                    }
                }
            }
            default -> { // week
                start = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
                end = now.with(DayOfWeek.SUNDAY).toLocalDate().atTime(23, 59, 59);
                prevStart = start.minusWeeks(1);
                prevEnd = end.minusWeeks(1);
                labels = List.of("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN");
                groupBy = "day";
            }
        }

        return AdRevenueChartResponse.builder()
                .timeLabels(labels)
                .currentData(aggregateData(start, end, labels, groupBy))
                .previousData(aggregateData(prevStart, prevEnd, labels, groupBy))
                .build();
    }


    // --- 3. CÁC API KHÁC ---
    @Override
    public List<AdChartResponse> getOrderStatusChart(String type, Long startCustom, Long endCustom) {
        Long startDate;
        Long endDate = System.currentTimeMillis();

        switch (type) {
            case "today":
                startDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                break;
            case "week":
                startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                break;
            case "month":
                startDate = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                break;
            case "year":
                startDate = LocalDate.now().withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                break;
            case "custom":
                startDate = startCustom;
                endDate = endCustom;
                break;
            default:
                startDate = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        return statisticsRepo.getFullOrderStatusStats(startDate, endDate);
    }

    @Override
    public List<AdChartResponse> getTopProductsChart(String type) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = type.equals("month") ? now.withDayOfMonth(1).toLocalDate().atStartOfDay() : now.minusDays(7).toLocalDate().atStartOfDay();
        return statisticsRepo.getTopSellingProductsChart(toMs(start), toMs(now), PageRequest.of(0, 10));
    }

    @Override
    public Page<AdProductResponse> getLowStockProducts(Integer limit, Pageable pageable) {
        return statisticsRepo.getLowStockProducts(limit, pageable);
    }

    @Override
    public List<AdGrowthStatResponse> getGrowthStatistics() {
        return new ArrayList<>();
    }

    // --- HELPERS ---

    private Long toLong(Object obj) {
        return obj == null ? 0L : ((Number) obj).longValue();
    }

    private Integer toInt(Object obj) {
        return obj == null ? 0 : ((Number) obj).intValue();
    }



    @Override
    public byte[] exportRevenueToExcel() { return new byte[0]; }
    private Long toMs(LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String getWeekdayLabel(LocalDateTime dt) {
        return switch (dt.getDayOfWeek()) {
            case MONDAY -> "Thứ 2"; case TUESDAY -> "Thứ 3"; case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5"; case FRIDAY -> "Thứ 6"; case SATURDAY -> "Thứ 7";
            case SUNDAY -> "CN";
        };
    }
    // Helper: Gom nhóm dữ liệu doanh thu
    private List<Long> aggregateData(LocalDateTime start, LocalDateTime end, List<String> labels, String groupBy) {
        List<AdChartResponse> raw = statisticsRepo.getRawRevenueData(toMs(start), toMs(end));
        Map<String, Long> map = new HashMap<>();

        for (AdChartResponse item : raw) {
            // Convert timestamp từ DB sang LocalDateTime
            LocalDateTime dt = Instant.ofEpochMilli(Long.parseLong(item.getName())).atZone(ZoneId.systemDefault()).toLocalDateTime();

            String key = "";
            if ("hour".equals(groupBy)) {
                key = dt.getHour() + "h";
            } else if ("month".equals(groupBy)) {
                key = "T" + dt.getMonthValue();
            } else { // day
                // Nếu labels là Thứ 2, Thứ 3 (Tuần)
                if (labels.contains("Thứ 2")) {
                    key = getWeekdayLabel(dt);
                } else {
                    // Nếu labels là 1, 2, 3 (Tháng hoặc Custom)
                    key = String.valueOf(dt.getDayOfMonth());
                }
            }
            map.put(key, map.getOrDefault(key, 0L) + item.getValue());
        }

        // Map dữ liệu vào labels để đảm bảo thứ tự
        return labels.stream().map(l -> map.getOrDefault(l, 0L)).collect(Collectors.toList());
    }

}
