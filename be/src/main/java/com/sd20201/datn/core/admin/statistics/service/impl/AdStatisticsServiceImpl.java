package com.sd20201.datn.core.admin.statistics.service.impl;

import com.sd20201.datn.core.admin.statistics.model.response.*;
import com.sd20201.datn.core.admin.statistics.repository.AdStatisticsRepository;
import com.sd20201.datn.core.admin.statistics.service.AdStatisticsService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdStatisticsServiceImpl implements AdStatisticsService {

    private final AdStatisticsRepository statisticsRepo;

    // =========================================================================
    // 1. TỔNG QUAN (4 CARDS: HÔM NAY, TUẦN, THÁNG, NĂM)
    // =========================================================================
    @Override
    public AdDashboardOverviewResponse getDashboardOverview() {
        LocalDateTime now = LocalDateTime.now();

        // --- A. Top 5 sản phẩm (Giữ nguyên) ---
        List<Object[]> rawTopProducts = statisticsRepo.getTopSellingProductsWithDetail();
        List<AdDashboardOverviewResponse.TopItemDTO> topProducts = rawTopProducts.stream()
                .map(row -> AdDashboardOverviewResponse.TopItemDTO.builder()
                        .name((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .price(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .image((String) row[3])
                        .build())
                .collect(Collectors.toList());

        // --- B. Định nghĩa thời gian ---
        // 1. HÔM NAY
        LocalDateTime startToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startYesterday = startToday.minusDays(1);
        LocalDateTime endYesterday = startToday.minusSeconds(1);

        // 2. TUẦN NÀY
        LocalDateTime startWeek = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        LocalDateTime startLastWeek = startWeek.minusWeeks(1);
        LocalDateTime endLastWeek = startWeek.minusSeconds(1);

        // 3. THÁNG NÀY
        LocalDateTime startMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startLastMonth = startMonth.minusMonths(1);
        LocalDateTime endLastMonth = startMonth.minusSeconds(1);

        // 4. NĂM NAY
        LocalDateTime startYear = now.withDayOfYear(1).toLocalDate().atStartOfDay();
        LocalDateTime startLastYear = startYear.minusYears(1);
        LocalDateTime endLastYear = startYear.minusSeconds(1);

        // --- C. Trả về kết quả ---
        return AdDashboardOverviewResponse.builder()
                .today(getStatsForPeriod(startToday, now, startYesterday, endYesterday))
                .week(getStatsForPeriod(startWeek, now, startLastWeek, endLastWeek))
                .month(getStatsForPeriod(startMonth, now, startLastMonth, endLastMonth))
                .year(getStatsForPeriod(startYear, now, startLastYear, endLastYear))
                .topSellingProducts(topProducts)
                .build();
    }

    /**
     * Helper: Tính toán số liệu cho 1 Card (Bao gồm cả so sánh tăng trưởng)
     */
    private AdDashboardOverviewResponse.StatisticCard getStatsForPeriod(
            LocalDateTime start, LocalDateTime end,
            LocalDateTime prevStart, LocalDateTime prevEnd) {

        // 1. Lấy số liệu KỲ HIỆN TẠI & KỲ TRƯỚC
        Map<String, Object> currentData = statisticsRepo.getStatsByPeriod(toMs(start), toMs(end));
        Map<String, Object> prevData = statisticsRepo.getStatsByPeriod(toMs(prevStart), toMs(prevEnd));

        long currentRevenue = toLong(currentData.get("revenue"));
        long prevRevenue = toLong(prevData.get("revenue"));

        // 2. Tính % Tăng trưởng doanh thu (Fix lỗi chia cho 0)
        double growth;
        if (prevRevenue == 0) {
            // Nếu kỳ trước = 0:
            // - Kỳ này > 0 => Tăng 100%
            // - Kỳ này = 0 => 0%
            growth = (currentRevenue > 0) ? 100.0 : 0.0;
        } else {
            growth = ((double) (currentRevenue - prevRevenue) / prevRevenue) * 100;
        }

        String growthStr = (growth > 0 ? "+" : "") + String.format("%.0f", growth) + "%";

        return AdDashboardOverviewResponse.StatisticCard.builder()
                .revenue(currentRevenue)
                .expectedRevenue(toLong(currentData.get("expectedRevenue"))) // Map cột mới
                .soldProducts(toInt(currentData.get("soldProducts")))
                .totalOrders(toInt(currentData.get("totalOrders")))
                .completed(toInt(currentData.get("completed")))
                .cancelled(toInt(currentData.get("cancelled")))
                .processing(toInt(currentData.get("processing")))
                .growthRate(growthStr)
                .build();
    }

    // =========================================================================
    // 2. BIỂU ĐỒ DOANH THU (LINE CHART)
    // =========================================================================
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
                if (startCustom == null || endCustom == null) {
                    start = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
                    end = now.with(DayOfWeek.SUNDAY).toLocalDate().atTime(23, 59, 59);
                } else {
                    start = Instant.ofEpochMilli(startCustom).atZone(ZoneId.systemDefault()).toLocalDateTime();
                    end = Instant.ofEpochMilli(endCustom).atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
                long daysDiff = ChronoUnit.DAYS.between(start, end);
                prevStart = start.minusDays(daysDiff);
                prevEnd = end.minusDays(daysDiff);

                if (daysDiff <= 1) {
                    for (int i = 0; i < 24; i++) labels.add(i + "h");
                    groupBy = "hour";
                } else if (daysDiff <= 31) {
                    for (int i = 0; i <= daysDiff; i++) labels.add(String.valueOf(start.plusDays(i).getDayOfMonth()));
                    groupBy = "day";
                } else {
                    groupBy = "month";
                    long months = ChronoUnit.MONTHS.between(start, end);
                    for (int i = 0; i <= months; i++) labels.add("T" + start.plusMonths(i).getMonthValue());
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

    private List<Long> aggregateData(LocalDateTime start, LocalDateTime end, List<String> labels, String groupBy) {
        List<AdChartResponse> raw = statisticsRepo.getRawRevenueData(toMs(start), toMs(end));
        Map<String, Long> map = new HashMap<>();

        for (AdChartResponse item : raw) {
            try {
                long timestamp = Long.parseLong(item.getName());
                LocalDateTime dt = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
                String key = "";
                if ("hour".equals(groupBy)) key = dt.getHour() + "h";
                else if ("month".equals(groupBy)) key = "T" + dt.getMonthValue();
                else {
                    if (labels.contains("Thứ 2") || labels.contains("CN")) key = getWeekdayLabel(dt);
                    else key = String.valueOf(dt.getDayOfMonth());
                }
                map.put(key, map.getOrDefault(key, 0L) + item.getValue());
            } catch (Exception ignored) { }
        }
        return labels.stream().map(l -> map.getOrDefault(l, 0L)).collect(Collectors.toList());
    }

    // =========================================================================
    // 3. TỐC ĐỘ TĂNG TRƯỞNG (CHI TIẾT)
    // =========================================================================
    @Override
    public List<AdGrowthStatResponse> getGrowthStatistics() {
        LocalDateTime now = LocalDateTime.now();
        List<AdGrowthStatResponse> list = new ArrayList<>();

        // Định nghĩa thời gian
        LocalDateTime startToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endToday = now;
        LocalDateTime startYest = startToday.minusDays(1);
        LocalDateTime endYest = endToday.minusDays(1);

        LocalDateTime startWeek = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        LocalDateTime endWeek = now;
        LocalDateTime startLastWeek = startWeek.minusWeeks(1);
        LocalDateTime endLastWeek = endWeek.minusWeeks(1);

        LocalDateTime startMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endMonth = now;
        LocalDateTime startLastMonth = startMonth.minusMonths(1);
        LocalDateTime endLastMonth = endMonth.minusMonths(1);

        LocalDateTime startYear = now.withDayOfYear(1).toLocalDate().atStartOfDay();
        LocalDateTime endYear = now;
        LocalDateTime startLastYear = startYear.minusYears(1);
        LocalDateTime endLastYear = endYear.minusYears(1);

        // DOANH THU
        list.add(createGrowthItem("Doanh thu ngày", startToday, endToday, startYest, endYest, "revenue"));
        list.add(createGrowthItem("Doanh thu tuần", startWeek, endWeek, startLastWeek, endLastWeek, "revenue"));
        list.add(createGrowthItem("Doanh thu tháng", startMonth, endMonth, startLastMonth, endLastMonth, "revenue"));
        list.add(createGrowthItem("Doanh thu năm", startYear, endYear, startLastYear, endLastYear, "revenue"));

        // ĐƠN HÀNG
        list.add(createGrowthItem("Đơn hàng ngày", startToday, endToday, startYest, endYest, "order"));
        list.add(createGrowthItem("Đơn hàng tuần", startWeek, endWeek, startLastWeek, endLastWeek, "order"));
        list.add(createGrowthItem("Đơn hàng tháng", startMonth, endMonth, startLastMonth, endLastMonth, "order"));
        list.add(createGrowthItem("Đơn hàng năm", startYear, endYear, startLastYear, endLastYear, "order"));

        // SẢN PHẨM
        list.add(createGrowthItem("Sản phẩm ngày", startToday, endToday, startYest, endYest, "product"));
        list.add(createGrowthItem("Sản phẩm tuần", startWeek, endWeek, startLastWeek, endLastWeek, "product"));
        list.add(createGrowthItem("Sản phẩm tháng", startMonth, endMonth, startLastMonth, endLastMonth, "product"));
        list.add(createGrowthItem("Sản phẩm năm", startYear, endYear, startLastYear, endLastYear, "product"));

        return list;
    }

    private AdGrowthStatResponse createGrowthItem(String label, LocalDateTime start, LocalDateTime end, LocalDateTime prevStart, LocalDateTime prevEnd, String type) {
        Map<String, Object> current = statisticsRepo.getStatsByPeriod(toMs(start), toMs(end));
        Map<String, Object> previous = statisticsRepo.getStatsByPeriod(toMs(prevStart), toMs(prevEnd));

        double currentVal = 0;
        double prevVal = 0;

        // Parse value
        if (type.equals("revenue")) {
            currentVal = toLong(current.get("revenue")).doubleValue();
            prevVal = toLong(previous.get("revenue")).doubleValue();
        } else if (type.equals("order")) {
            currentVal = toInt(current.get("totalOrders")).doubleValue();
            prevVal = toInt(previous.get("totalOrders")).doubleValue();
        } else {
            currentVal = toInt(current.get("soldProducts")).doubleValue();
            prevVal = toInt(previous.get("soldProducts")).doubleValue();
        }

        // --- LOGIC TÍNH % (FIX LỖI FULL ĐỎ) ---
        double percent = 0.0;
        boolean isIncrease = true; // Mặc định là Tăng (Xanh)

        if (prevVal == 0) {
            // Nếu kỳ trước = 0
            if (currentVal == 0) {
                percent = 0.0;     // 0 -> 0: Không đổi
                isIncrease = true; // Xanh
            } else {
                percent = 100.0;   // 0 -> Có: Tăng tuyệt đối
                isIncrease = true; // Xanh
            }
        } else {
            // Công thức chuẩn: ((Hiện tại - Quá khứ) / Quá khứ) * 100
            percent = ((currentVal - prevVal) / prevVal) * 100;
            isIncrease = percent >= 0;
        }

        String valueDisplay = type.equals("revenue") ? formatCurrency(currentVal) : String.valueOf((long) currentVal);

        return AdGrowthStatResponse.builder()
                .label(label)
                .value(valueDisplay)
                .percent(Math.abs(percent))
                .isIncrease(isIncrease) // Kết hợp với @JsonProperty ở DTO để gửi đúng
                .type(type)
                .build();
    }

    // =========================================================================
    // 4. XUẤT EXCEL
    // =========================================================================
    @Override
    public byte[] exportRevenueToExcel() throws IOException {
        LocalDateTime now = LocalDateTime.now();

        AdDashboardOverviewResponse.StatisticCard today = getStatsForPeriod(now.toLocalDate().atStartOfDay(), now, now.minusDays(1), now.minusDays(1));
        AdDashboardOverviewResponse.StatisticCard week = getStatsForPeriod(now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay(), now, now.minusWeeks(1), now.minusWeeks(1));
        AdDashboardOverviewResponse.StatisticCard month = getStatsForPeriod(now.withDayOfMonth(1).toLocalDate().atStartOfDay(), now, now.minusMonths(1), now.minusMonths(1));
        AdDashboardOverviewResponse.StatisticCard year = getStatsForPeriod(now.withDayOfYear(1).toLocalDate().atStartOfDay(), now, now.minusYears(1), now.minusYears(1));

        List<Object[]> rawTopProducts = statisticsRepo.getTopSellingProductsWithDetail();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo Cáo Doanh Thu");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0 ₫"));

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.createCell(0).setCellValue("BÁO CÁO DOANH THU");
            titleRow.getCell(0).setCellStyle(headerStyle);
            rowIdx++;

            Row header1 = sheet.createRow(rowIdx++);
            String[] cols1 = {"Thời gian", "Doanh thu", "Đơn hàng", "SP Bán", "Hoàn thành", "Hủy"};
            for(int i=0; i<cols1.length; i++) {
                Cell c = header1.createCell(i); c.setCellValue(cols1[i]); c.setCellStyle(headerStyle);
            }

            Object[][] data = {
                    {"Hôm nay", today.getRevenue(), today.getTotalOrders(), today.getSoldProducts(), today.getCompleted(), today.getCancelled()},
                    {"Tuần này", week.getRevenue(), week.getTotalOrders(), week.getSoldProducts(), week.getCompleted(), week.getCancelled()},
                    {"Tháng này", month.getRevenue(), month.getTotalOrders(), month.getSoldProducts(), month.getCompleted(), month.getCancelled()},
                    {"Năm nay", year.getRevenue(), year.getTotalOrders(), year.getSoldProducts(), year.getCompleted(), year.getCancelled()}
            };

            for(Object[] r : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue((String)r[0]);
                Cell c1 = row.createCell(1); c1.setCellValue(((Number)r[1]).doubleValue()); c1.setCellStyle(currencyStyle);
                row.createCell(2).setCellValue(((Number)r[2]).doubleValue());
                row.createCell(3).setCellValue(((Number)r[3]).doubleValue());
                row.createCell(4).setCellValue(((Number)r[4]).doubleValue());
                row.createCell(5).setCellValue(((Number)r[5]).doubleValue());
            }

            rowIdx += 2;
            Row titleRow2 = sheet.createRow(rowIdx++);
            titleRow2.createCell(0).setCellValue("TOP SẢN PHẨM BÁN CHẠY");
            titleRow2.getCell(0).setCellStyle(headerStyle);

            Row header2 = sheet.createRow(rowIdx++);
            String[] cols2 = {"Tên sản phẩm", "Số lượng bán", "Giá hiện tại"};
            for(int i=0; i<cols2.length; i++) {
                Cell c = header2.createCell(i); c.setCellValue(cols2[i]); c.setCellStyle(headerStyle);
            }

            for(Object[] p : rawTopProducts) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue((String)p[0]);
                row.createCell(1).setCellValue(((Number)p[1]).doubleValue());
                Cell cPrice = row.createCell(2);
                cPrice.setCellValue(p[2] != null ? ((Number)p[2]).doubleValue() : 0);
                cPrice.setCellStyle(currencyStyle);
            }

            for(int i=0; i<6; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // =========================================================================
    // 5. CÁC API KHÁC (PASSTHROUGH) & UTILS
    // =========================================================================
    @Override
    public List<AdChartResponse> getOrderStatusChart(String type, Long startCustom, Long endCustom) {
        Long startDate;
        Long endDate = System.currentTimeMillis();
        switch (type) {
            case "today" -> startDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            case "week" -> startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            case "month" -> startDate = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            case "year" -> startDate = LocalDate.now().withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            case "custom" -> { startDate = startCustom; endDate = endCustom; }
            default -> startDate = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
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

    private Long toMs(LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    private Long toLong(Object obj) { return obj == null ? 0L : ((Number) obj).longValue(); }
    private Integer toInt(Object obj) { return obj == null ? 0 : ((Number) obj).intValue(); }
    private String formatCurrency(double value) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(value);
    }
    private String getWeekdayLabel(LocalDateTime dt) {
        return switch (dt.getDayOfWeek()) {
            case MONDAY -> "Thứ 2"; case TUESDAY -> "Thứ 3"; case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5"; case FRIDAY -> "Thứ 6"; case SATURDAY -> "Thứ 7";
            case SUNDAY -> "CN";
        };
    }
}