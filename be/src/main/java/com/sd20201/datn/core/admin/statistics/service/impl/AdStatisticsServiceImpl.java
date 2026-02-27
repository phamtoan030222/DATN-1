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

        List<Object[]> rawTopProducts = statisticsRepo.getTopSellingProductsWithDetail();
        List<AdDashboardOverviewResponse.TopItemDTO> topProducts = rawTopProducts.stream()
                .map(row -> AdDashboardOverviewResponse.TopItemDTO.builder()
                        .name((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .price(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .image((String) row[3])
                        .build())
                .collect(Collectors.toList());

        LocalDateTime startToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startYesterday = startToday.minusDays(1);
        LocalDateTime endYesterday = startToday.minusSeconds(1);

        LocalDateTime startWeek = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        LocalDateTime startLastWeek = startWeek.minusWeeks(1);
        LocalDateTime endLastWeek = startWeek.minusSeconds(1);

        LocalDateTime startMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startLastMonth = startMonth.minusMonths(1);
        LocalDateTime endLastMonth = startMonth.minusSeconds(1);

        int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
        int startMonthOfQ = (currentQuarter - 1) * 3 + 1;
        LocalDateTime startQuarter = now.withMonth(startMonthOfQ).withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startLastQuarter = startQuarter.minusMonths(3);
        LocalDateTime endLastQuarter = startQuarter.minusSeconds(1);

        LocalDateTime startYear = now.withDayOfYear(1).toLocalDate().atStartOfDay();
        LocalDateTime startLastYear = startYear.minusYears(1);
        LocalDateTime endLastYear = startYear.minusSeconds(1);

        return AdDashboardOverviewResponse.builder()
                .today(getStatsForPeriod(startToday, now, startYesterday, endYesterday))
                .week(getStatsForPeriod(startWeek, now, startLastWeek, endLastWeek))
                .month(getStatsForPeriod(startMonth, now, startLastMonth, endLastMonth))
                .quarter(getStatsForPeriod(startQuarter, now, startLastQuarter, endLastQuarter))
                .year(getStatsForPeriod(startYear, now, startLastYear, endLastYear))
                .topSellingProducts(topProducts)
                .build();
    }

    private AdDashboardOverviewResponse.StatisticCard getStatsForPeriod(
            LocalDateTime start, LocalDateTime end,
            LocalDateTime prevStart, LocalDateTime prevEnd) {

        Map<String, Object> currentData = statisticsRepo.getStatsByPeriod(toMs(start), toMs(end));
        Map<String, Object> prevData = statisticsRepo.getStatsByPeriod(toMs(prevStart), toMs(prevEnd));

        long currentRevenue = toLong(currentData.get("revenue"));
        long prevRevenue = toLong(prevData.get("revenue"));

        double growth;
        if (prevRevenue == 0) {
            growth = (currentRevenue > 0) ? 100.0 : 0.0;
        } else {
            growth = ((double) (currentRevenue - prevRevenue) / prevRevenue) * 100;
        }

        String growthStr = (growth > 0 ? "+" : "") + String.format("%.0f", growth) + "%";

        return AdDashboardOverviewResponse.StatisticCard.builder()
                .revenue(currentRevenue)
                .expectedRevenue(toLong(currentData.get("expectedRevenue")))
                .soldProducts(toInt(currentData.get("soldProducts")))
                .totalOrders(toInt(currentData.get("totalOrders")))
                .completed(toInt(currentData.get("completed")))
                .cancelled(toInt(currentData.get("cancelled")))
                .processing(toInt(currentData.get("processing")))
                .growthRate(growthStr)
                .build();
    }


    // 2. BIỂU ĐỒ DOANH THU (LINE CHART)
    @Override
    public AdRevenueChartResponse getRevenueChart(String type, Long startCustom, Long endCustom) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start, end, prevStart, prevEnd;
        List<String> labels = new ArrayList<>();
        String groupBy;

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
            case "quarter" -> {
                int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int startMonthOfQ = (currentQuarter - 1) * 3 + 1;
                LocalDate firstDayOfQ = now.toLocalDate().withMonth(startMonthOfQ).withDayOfMonth(1);
                start = firstDayOfQ.atStartOfDay();
                LocalDate lastDayOfQ = firstDayOfQ.plusMonths(2).withDayOfMonth(firstDayOfQ.plusMonths(2).lengthOfMonth());
                end = lastDayOfQ.atTime(23, 59, 59);

                LocalDate prevFirstDayOfQ = firstDayOfQ.minusMonths(3);
                prevStart = prevFirstDayOfQ.atStartOfDay();
                LocalDate prevLastDayOfQ = prevFirstDayOfQ.plusMonths(2).withDayOfMonth(prevFirstDayOfQ.plusMonths(2).lengthOfMonth());
                prevEnd = prevLastDayOfQ.atTime(23, 59, 59);

                for (int i = 0; i < 3; i++) labels.add("T" + firstDayOfQ.plusMonths(i).getMonthValue());
                groupBy = "month";
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
    // 3. TỐC ĐỘ TĂNG TRƯỞNG (ĐÃ FIX LỖI 97%)
    // =========================================================================
    @Override
    public List<AdGrowthStatResponse> getGrowthStatistics() {
        LocalDateTime now = LocalDateTime.now();
        List<AdGrowthStatResponse> list = new ArrayList<>();

        // NGÀY (Lấy đủ 24h hôm qua)
        LocalDateTime startToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endToday = now;
        LocalDateTime startYest = startToday.minusDays(1);
        LocalDateTime endYest = startToday.minusSeconds(1);

        // TUẦN (Lấy trọn vẹn tuần trước)
        LocalDateTime startWeek = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        LocalDateTime endWeek = now;
        LocalDateTime startLastWeek = startWeek.minusWeeks(1);
        LocalDateTime endLastWeek = startWeek.minusSeconds(1);

        // THÁNG
        LocalDateTime startMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endMonth = now;
        LocalDateTime startLastMonth = startMonth.minusMonths(1);
        LocalDateTime endLastMonth = startMonth.minusSeconds(1);

        // QUÝ
        int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
        int startMonthOfQ = (currentQuarter - 1) * 3 + 1;
        LocalDateTime startQuarter = now.withMonth(startMonthOfQ).withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endQuarter = now;
        LocalDateTime startLastQuarter = startQuarter.minusMonths(3);
        LocalDateTime endLastQuarter = startQuarter.minusSeconds(1);

        // NĂM
        LocalDateTime startYear = now.withDayOfYear(1).toLocalDate().atStartOfDay();
        LocalDateTime endYear = now;
        LocalDateTime startLastYear = startYear.minusYears(1);
        LocalDateTime endLastYear = startYear.minusSeconds(1);

        // DOANH THU
        list.add(createGrowthItem("Doanh thu ngày", startToday, endToday, startYest, endYest, "revenue"));
        list.add(createGrowthItem("Doanh thu tuần", startWeek, endWeek, startLastWeek, endLastWeek, "revenue"));
        list.add(createGrowthItem("Doanh thu tháng", startMonth, endMonth, startLastMonth, endLastMonth, "revenue"));
        list.add(createGrowthItem("Doanh thu quý", startQuarter, endQuarter, startLastQuarter, endLastQuarter, "revenue"));
        list.add(createGrowthItem("Doanh thu năm", startYear, endYear, startLastYear, endLastYear, "revenue"));

        // ĐƠN HÀNG
        list.add(createGrowthItem("Đơn hàng ngày", startToday, endToday, startYest, endYest, "order"));
        list.add(createGrowthItem("Đơn hàng tuần", startWeek, endWeek, startLastWeek, endLastWeek, "order"));
        list.add(createGrowthItem("Đơn hàng tháng", startMonth, endMonth, startLastMonth, endLastMonth, "order"));
        list.add(createGrowthItem("Đơn hàng quý", startQuarter, endQuarter, startLastQuarter, endLastQuarter, "order"));
        list.add(createGrowthItem("Đơn hàng năm", startYear, endYear, startLastYear, endLastYear, "order"));

        // SẢN PHẨM
        list.add(createGrowthItem("Sản phẩm ngày", startToday, endToday, startYest, endYest, "product"));
        list.add(createGrowthItem("Sản phẩm tuần", startWeek, endWeek, startLastWeek, endLastWeek, "product"));
        list.add(createGrowthItem("Sản phẩm tháng", startMonth, endMonth, startLastMonth, endLastMonth, "product"));
        list.add(createGrowthItem("Sản phẩm quý", startQuarter, endQuarter, startLastQuarter, endLastQuarter, "product"));
        list.add(createGrowthItem("Sản phẩm năm", startYear, endYear, startLastYear, endLastYear, "product"));

        return list;
    }

    @Override
    public byte[] exportRevenueToExcel(String type, Long startCustom, Long endCustom) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        long startMs, endMs = toMs(now);
        String periodName = "";

        if ("custom".equals(type) && startCustom != null && endCustom != null) {
            startMs = startCustom; endMs = endCustom; periodName = "Tùy chỉnh";
        } else {
            if ("today".equals(type)) {
                startMs = toMs(now.toLocalDate().atStartOfDay()); periodName = "Hôm nay";
            } else if ("week".equals(type)) {
                startMs = toMs(now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay()); periodName = "Tuần này";
            } else if ("quarter".equals(type)) {
                int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int startMonthOfQ = (currentQuarter - 1) * 3 + 1;
                startMs = toMs(now.withMonth(startMonthOfQ).withDayOfMonth(1).toLocalDate().atStartOfDay());
                periodName = "Quý này";
            } else if ("year".equals(type)) {
                startMs = toMs(now.withDayOfYear(1).toLocalDate().atStartOfDay()); periodName = "Năm nay";
            } else {
                startMs = toMs(now.withDayOfMonth(1).toLocalDate().atStartOfDay()); periodName = "Tháng này";
            }
        }

        Map<String, Object> overviewData = statisticsRepo.getStatsByPeriod(startMs, endMs);
        List<Object[]> topProducts = statisticsRepo.getTopSellingProductsByDateRange(startMs, endMs);
        Page<AdProductResponse> lowStockPage = statisticsRepo.getLowStockProducts(10, PageRequest.of(0, 10));

        AdRevenueChartResponse chartData = getRevenueChart(type, startCustom, endCustom);
        List<String> timeLabels = chartData.getTimeLabels();
        List<Long> revenues = chartData.getCurrentData();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bao Cao Thong Ke");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont(); headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN); headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN); headerStyle.setBorderLeft(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0 ₫"));

            int rowIdx = 0;

            Row titleRow1 = sheet.createRow(rowIdx++);
            titleRow1.createCell(0).setCellValue("1. TỔNG QUAN VẬN HÀNH (" + periodName.toUpperCase() + ")");
            titleRow1.getCell(0).setCellStyle(headerStyle);

            Row header1 = sheet.createRow(rowIdx++);
            String[] cols1 = {"Thời gian", "Tổng đơn hàng", "Sản phẩm bán", "Hoàn thành", "Hủy", "Đang xử lý", "Doanh thu", "DT Dự kiến"};
            for(int i=0; i<cols1.length; i++) {
                Cell c = header1.createCell(i); c.setCellValue(cols1[i]); c.setCellStyle(headerStyle);
            }

            Row dataRow1 = sheet.createRow(rowIdx++);
            dataRow1.createCell(0).setCellValue(periodName);
            dataRow1.createCell(1).setCellValue(toInt(overviewData.get("totalOrders")));
            dataRow1.createCell(2).setCellValue(toInt(overviewData.get("soldProducts")));
            dataRow1.createCell(3).setCellValue(toInt(overviewData.get("completed")));
            dataRow1.createCell(4).setCellValue(toInt(overviewData.get("cancelled")));
            dataRow1.createCell(5).setCellValue(toInt(overviewData.get("processing")));
            Cell cRev = dataRow1.createCell(6); cRev.setCellValue(toLong(overviewData.get("revenue"))); cRev.setCellStyle(currencyStyle);
            Cell cExp = dataRow1.createCell(7); cExp.setCellValue(toLong(overviewData.get("expectedRevenue"))); cExp.setCellStyle(currencyStyle);

            rowIdx += 2;

            Row titleRowChart = sheet.createRow(rowIdx++);
            titleRowChart.createCell(0).setCellValue("2. CHI TIẾT DOANH THU");
            titleRowChart.getCell(0).setCellStyle(headerStyle);

            Row headerChart = sheet.createRow(rowIdx++);
            Cell hc1 = headerChart.createCell(0); hc1.setCellValue("Thời gian"); hc1.setCellStyle(headerStyle);
            Cell hc2 = headerChart.createCell(1); hc2.setCellValue("Doanh thu"); hc2.setCellStyle(headerStyle);

            for (int i = 0; i < timeLabels.size(); i++) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(timeLabels.get(i));
                Cell valCell = r.createCell(1);
                valCell.setCellValue(revenues.get(i));
                valCell.setCellStyle(currencyStyle);
            }

            rowIdx += 2;

            Row titleRow2 = sheet.createRow(rowIdx++);
            titleRow2.createCell(0).setCellValue("3. TOP 5 SẢN PHẨM BÁN CHẠY NHẤT");
            titleRow2.getCell(0).setCellStyle(headerStyle);

            Row header2 = sheet.createRow(rowIdx++);
            String[] cols2 = {"#", "Tên sản phẩm", "Số lượng bán", "Giá hiện tại"};
            for(int i=0; i<cols2.length; i++) {
                Cell c = header2.createCell(i); c.setCellValue(cols2[i]); c.setCellStyle(headerStyle);
            }

            if (topProducts.isEmpty()) {
                sheet.createRow(rowIdx++).createCell(1).setCellValue("Không có dữ liệu bán hàng kỳ này");
            } else {
                int r2 = 1;
                for(Object[] p : topProducts) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(r2++);
                    row.createCell(1).setCellValue((String)p[0]);
                    row.createCell(2).setCellValue(p[1] != null ? ((Number)p[1]).doubleValue() : 0);
                    Cell cPrice = row.createCell(3); cPrice.setCellValue(p[2] != null ? ((Number)p[2]).doubleValue() : 0); cPrice.setCellStyle(currencyStyle);
                }
            }

            rowIdx += 2;

            Row titleRow3 = sheet.createRow(rowIdx++);
            titleRow3.createCell(0).setCellValue("4. CẢNH BÁO SẢN PHẨM SẮP HẾT (TỒN KHO <= 10)");
            titleRow3.getCell(0).setCellStyle(headerStyle);

            Row header3 = sheet.createRow(rowIdx++);
            String[] cols3 = {"#", "Tên sản phẩm", "Thương hiệu", "Tồn kho"};
            for(int i=0; i<cols3.length; i++) {
                Cell c = header3.createCell(i); c.setCellValue(cols3[i]); c.setCellStyle(headerStyle);
            }

            List<AdProductResponse> lowStockList = lowStockPage.getContent();
            if (lowStockList.isEmpty()) {
                sheet.createRow(rowIdx++).createCell(1).setCellValue("Không có sản phẩm nào sắp hết hàng");
            } else {
                int r3 = 1;
                for(AdProductResponse p : lowStockList) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(r3++);
                    row.createCell(1).setCellValue(p.getName());
                    row.createCell(2).setCellValue(p.getBrandName() != null ? p.getBrandName() : "N/A");
                    row.createCell(3).setCellValue(p.getQuantity());
                }
            }

            for(int i=0; i<8; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private AdGrowthStatResponse createGrowthItem(String label, LocalDateTime start, LocalDateTime end, LocalDateTime prevStart, LocalDateTime prevEnd, String type) {
        Map<String, Object> current = statisticsRepo.getStatsByPeriod(toMs(start), toMs(end));
        Map<String, Object> previous = statisticsRepo.getStatsByPeriod(toMs(prevStart), toMs(prevEnd));

        double currentVal = type.equals("revenue") ? toLong(current.get("revenue")).doubleValue() : (type.equals("order") ? toInt(current.get("totalOrders")).doubleValue() : toInt(current.get("soldProducts")).doubleValue());
        double prevVal = type.equals("revenue") ? toLong(previous.get("revenue")).doubleValue() : (type.equals("order") ? toInt(previous.get("totalOrders")).doubleValue() : toInt(previous.get("soldProducts")).doubleValue());

        double percent = 0.0;
        boolean isIncrease = true;

        if (prevVal == 0) {
            if (currentVal == 0) {
                percent = 0.0;
            } else {
                percent = 100.0;
            }
        } else {
            percent = ((currentVal - prevVal) / prevVal) * 100;
            isIncrease = percent >= 0;
        }

        String valueDisplay = type.equals("revenue") ? formatCurrency(currentVal) : String.valueOf((long) currentVal);

        return AdGrowthStatResponse.builder()
                .label(label)
                .value(valueDisplay)
                .percent(Math.abs(percent))
                .isIncrease(isIncrease)
                .type(type)
                .build();
    }

    @Override
    public List<AdDashboardOverviewResponse.TopItemDTO> getTopProductsByFilter(String type, Long startCustom, Long endCustom) {
        LocalDateTime now = LocalDateTime.now();
        long startMs, endMs = toMs(now);

        if ("custom".equals(type) && startCustom != null && endCustom != null) {
            startMs = startCustom;
            endMs = endCustom;
        } else {
            if ("today".equals(type)) {
                startMs = toMs(now.toLocalDate().atStartOfDay());
            } else if ("week".equals(type)) {
                startMs = toMs(now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay());
            } else if ("quarter".equals(type)) {
                int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int startMonthOfQ = (currentQuarter - 1) * 3 + 1;
                startMs = toMs(now.withMonth(startMonthOfQ).withDayOfMonth(1).toLocalDate().atStartOfDay());
            } else if ("year".equals(type)) {
                startMs = toMs(now.withDayOfYear(1).toLocalDate().atStartOfDay());
            } else {
                startMs = toMs(now.withDayOfMonth(1).toLocalDate().atStartOfDay());
            }
        }
        return getTopSellingProductsByDateRange(startMs, endMs);
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
            case "quarter" -> {
                int currentQuarter = (LocalDate.now().getMonthValue() - 1) / 3 + 1;
                int startMonthOfQ = (currentQuarter - 1) * 3 + 1;
                startDate = LocalDate.now().withMonth(startMonthOfQ).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            case "year" -> startDate = LocalDate.now().withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            case "custom" -> { startDate = startCustom; endDate = endCustom; }
            default -> startDate = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return statisticsRepo.getFullOrderStatusStats(startDate, endDate);
    }

    @Override
    public List<AdDashboardOverviewResponse.TopItemDTO> getTopSellingProductsByDateRange(Long start, Long end) {
        List<Object[]> rawList = statisticsRepo.getTopSellingProductsByDateRange(start, end);

        return rawList.stream().map(row -> AdDashboardOverviewResponse.TopItemDTO.builder()
                .name((String) row[0])
                .count(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                .price(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                .image((String) row[3])
                .build()).collect(Collectors.toList());
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