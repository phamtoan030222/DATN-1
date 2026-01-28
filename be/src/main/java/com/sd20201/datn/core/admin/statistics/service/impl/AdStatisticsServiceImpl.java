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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AdStatisticsServiceImpl implements AdStatisticsService {

    private final AdStatisticsRepository statisticsRepo;


    // --- 1. TỔNG QUAN (4 CARDS) ---
    @Override
    public AdDashboardOverviewResponse getDashboardOverview() {
        LocalDateTime now = LocalDateTime.now();

        // --- A. Lấy danh sách Top 5 sản phẩm (Giữ nguyên) ---
        List<Object[]> rawTopProducts = statisticsRepo.getTopSellingProductsWithDetail();
        List<AdDashboardOverviewResponse.TopItemDTO> topProducts = rawTopProducts.stream()
                .map(row -> AdDashboardOverviewResponse.TopItemDTO.builder()
                        .name((String) row[0])
                        .count(((Number) row[1]).longValue())
                        // Xử lý null an toàn cho giá tiền
                        .price(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .image((String) row[3])
                        .build())
                .collect(Collectors.toList());

        // --- B. Tính toán thời gian cho 4 thẻ (Card) ---

        // 1. HÔM NAY (So với Hôm qua)
        // Kỳ này: Từ 00:00 hôm nay đến hiện tại
        LocalDateTime startToday = now.toLocalDate().atStartOfDay();
        // Kỳ trước: Từ 00:00 hôm qua đến 23:59 hôm qua (trước khi sang hôm nay)
        LocalDateTime startYesterday = startToday.minusDays(1);
        LocalDateTime endYesterday = startToday.minusSeconds(1);

        // 2. TUẦN NÀY (So với Tuần trước)
        // Kỳ này: Từ Thứ 2 tuần này
        LocalDateTime startWeek = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        // Kỳ trước: Từ Thứ 2 tuần trước
        LocalDateTime startLastWeek = startWeek.minusWeeks(1);
        LocalDateTime endLastWeek = startWeek.minusSeconds(1);

        // 3. THÁNG NÀY (So với Tháng trước)
        // Kỳ này: Từ ngày mùng 1 tháng này
        LocalDateTime startMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        // Kỳ trước: Từ ngày mùng 1 tháng trước
        LocalDateTime startLastMonth = startMonth.minusMonths(1);
        LocalDateTime endLastMonth = startMonth.minusSeconds(1);

        // 4. NĂM NAY (So với Năm trước)
        // Kỳ này: Từ ngày 1/1 năm nay
        LocalDateTime startYear = now.withDayOfYear(1).toLocalDate().atStartOfDay();
        // Kỳ trước: Từ ngày 1/1 năm trước
        LocalDateTime startLastYear = startYear.minusYears(1);
        LocalDateTime endLastYear = startYear.minusSeconds(1);

        // --- C. Truyền đủ 4 tham số vào hàm tính toán ---
        return AdDashboardOverviewResponse.builder()
                .today(getStatsForPeriod(startToday, now, startYesterday, endYesterday))
                .week(getStatsForPeriod(startWeek, now, startLastWeek, endLastWeek))
                .month(getStatsForPeriod(startMonth, now, startLastMonth, endLastMonth))
                .year(getStatsForPeriod(startYear, now, startLastYear, endLastYear))
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
    public byte[] exportRevenueToExcel() throws IOException{
        LocalDateTime now = LocalDateTime.now();

        // Lấy dữ liệu 4 mốc thời gian
        AdDashboardOverviewResponse.StatisticCard today = getStatsForPeriod(now.toLocalDate().atStartOfDay(), now);
        AdDashboardOverviewResponse.StatisticCard week = getStatsForPeriod(now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay(), now);
        AdDashboardOverviewResponse.StatisticCard month = getStatsForPeriod(now.withDayOfMonth(1).toLocalDate().atStartOfDay(), now);
        AdDashboardOverviewResponse.StatisticCard year = getStatsForPeriod(now.withDayOfYear(1).toLocalDate().atStartOfDay(), now);

        // Lấy Top sản phẩm
        List<Object[]> rawTopProducts = statisticsRepo.getTopSellingProductsWithDetail();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo Cáo Doanh Thu");

            // --- STYLES ---
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

            // --- BẢNG TỔNG QUAN ---
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

            // --- BẢNG TOP SẢN PHẨM ---
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

    /**
     * HÀM PHỤ TRỢ: Tính chỉ số và % tăng trưởng
     * @param start : Bắt đầu kỳ hiện tại
     * @param end : Kết thúc kỳ hiện tại
     * @param prevStart : Bắt đầu kỳ trước (để so sánh)
     * @param prevEnd : Kết thúc kỳ trước
     */
    private AdDashboardOverviewResponse.StatisticCard getStatsForPeriod(
            LocalDateTime start, LocalDateTime end,
            LocalDateTime prevStart, LocalDateTime prevEnd) {

        // 1. Lấy số liệu KỲ HIỆN TẠI
        Map<String, Object> currentData = statisticsRepo.getStatsByPeriod(toMs(start), toMs(end));
        long currentRevenue = toLong(currentData.get("revenue"));

        // 2. Lấy số liệu KỲ TRƯỚC (QUÁ KHỨ)
        Map<String, Object> prevData = statisticsRepo.getStatsByPeriod(toMs(prevStart), toMs(prevEnd));
        long prevRevenue = toLong(prevData.get("revenue"));

        // 3. Tính % Tăng trưởng doanh thu
        double growth;
        if (prevRevenue == 0) {
            // Nếu kỳ trước = 0, kỳ này > 0 => Tăng trưởng 100% (tăng tuyệt đối)
            // Nếu cả 2 kỳ = 0 => 0%
            growth = (currentRevenue > 0) ? 100.0 : 0.0;
        } else {
            // Công thức: ((Hiện tại - Quá khứ) / Quá khứ) * 100
            growth = ((double) (currentRevenue - prevRevenue) / prevRevenue) * 100;
        }

        // 4. Format chuỗi hiển thị (VD: "+100%", "-50%", "0%")
        String growthStr = (growth > 0 ? "+" : "") + String.format("%.0f", growth) + "%";

        return AdDashboardOverviewResponse.StatisticCard.builder()
                .revenue(currentRevenue)
                .soldProducts(toInt(currentData.get("soldProducts")))
                .totalOrders(toInt(currentData.get("totalOrders")))
                .completed(toInt(currentData.get("completed")))
                .cancelled(toInt(currentData.get("cancelled")))
                .processing(toInt(currentData.get("processing")))
                .growthRate(growthStr) // Gán giá trị tính toán được vào đây
                .build();
    }

}
