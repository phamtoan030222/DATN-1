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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
                .successfulOrders(invoiceRepo.countByStatus(1))
                .pendingOrders(invoiceRepo.countByStatus(3))
                .processingOrders(invoiceRepo.countByStatus(0))
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

        // logic ngày tháng chuẩn
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

    // BIỂU ĐỒ DOANH THU
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
                        key = String.valueOf(dt.getHour());
                    } else if ("month".equals(groupBy)) {
                        key = String.valueOf(dt.getMonthValue());
                    } else {
                        key = dt.toLocalDate().toString();
                    }

                    dataMap.put(key, dataMap.getOrDefault(key, 0L) + amount);
                } catch (Exception e) { continue; }
            }
        }

        List<Long> result = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            String keyCheck = "";
            if ("hour".equals(groupBy)) {
                keyCheck = String.valueOf(i);
            } else if ("month".equals(groupBy)) {
                keyCheck = String.valueOf(i + 1);
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

    @Override
    public byte[] exportRevenueToExcel() throws IOException {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusYears(1);
        List<AdChartResponse> revenueData = invoiceRepo.getRawRevenueData(toMilis(start), toMilis(end));
        List<AdChartResponse> topSelling = productRepo.getTopSellingProductsChart(toMilis(start), toMilis(end), PageRequest.of(0, 20));
        List<AdProductResponse> lowStock = productRepo.getLowStockProducts(10, PageRequest.of(0, 100)).getContent();
        List<AdChartResponse> topBrands = productRepo.getTop3Brands();
        List<Object[]> recentOrders = invoiceRepo.getRecentOrdersForExport();

        List<Long> customerDates = customerRepo.getAllCustomerCreatedDates();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            createRevenueSheet(workbook, revenueData);
            createTopSellingSheet(workbook, topSelling);
            createLowStockSheet(workbook, lowStock);
            createTopBrandsSheet(workbook, topBrands);
            createOrdersSheet(workbook, recentOrders);

            createCustomersSheet(workbook, customerDates);

            workbook.write(out);
            return out.toByteArray();
        }
    }


    private void createCustomersSheet(Workbook wb, List<Long> dates) {
        Sheet sheet = wb.createSheet("Tăng Trưởng Khách Hàng");

        // Header
        createHeader(wb, sheet, new String[]{"Tháng", "Số Khách Mới Đăng Ký"});

        // Map dùng để đếm: Key="01/2026", Value=Số lượng
        Map<String, Integer> countMap = new LinkedHashMap<>();

        if (dates != null) {
            for (Long timestamp : dates) {
                if(timestamp == null) continue;
                // Convert Timestamp -> LocalDate
                LocalDate date = Instant.ofEpochMilli(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                // Tạo Key định dạng "MM/yyyy"
                String key = String.format("%02d/%d", date.getMonthValue(), date.getYear());
                // Cộng dồn số lượng vào Map
                countMap.put(key, countMap.getOrDefault(key, 0) + 1);
            }
        }

        // Ghi dữ liệu từ Map vào Excel
        int rowIdx = 1;
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            // Cột 1: Tháng (Key)
            row.createCell(0).setCellValue(entry.getKey());
            // Cột 2: Số lượng (Value)
            row.createCell(1).setCellValue(entry.getValue());
        }
        // Auto resize cột
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createRevenueSheet(Workbook wb, List<AdChartResponse> data) {
        Sheet sheet = wb.createSheet("Doanh Thu");
        createHeader(wb, sheet, new String[]{"STT", "Ngày", "Doanh Thu (VNĐ)"});

        int rowIdx = 1;
        long total = 0;
        CellStyle currencyStyle = createCurrencyStyle(wb);

        if (data != null) {
            for (AdChartResponse item : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);

                // Parse ngày từ timestamp
                try {
                    long timestamp = Long.parseLong(item.getName());
                    LocalDate date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
                    row.createCell(1).setCellValue(date.toString());
                } catch (Exception e) {
                    row.createCell(1).setCellValue(item.getName());
                }

                Long val = item.getValue() == null ? 0L : item.getValue();
                total += val;
                Cell money = row.createCell(2);
                money.setCellValue(val);
                money.setCellStyle(currencyStyle);
            }
        }
        // Dòng Tổng cộng
        Row totalRow = sheet.createRow(rowIdx);
        totalRow.createCell(1).setCellValue("TỔNG CỘNG:");
        Cell totalVal = totalRow.createCell(2);
        totalVal.setCellValue(total);
        totalVal.setCellStyle(currencyStyle);

        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(2, 5000);
    }

    // 2. Sheet SP Bán Chạy
    private void createTopSellingSheet(Workbook wb, List<AdChartResponse> data) {
        Sheet sheet = wb.createSheet("Bán Chạy");
        createHeader(wb, sheet, new String[]{"Hạng", "Tên Sản Phẩm", "Số Lượng Bán"});

        int rowIdx = 1;
        if (data != null) {
            for (AdChartResponse item : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(item.getName());
                row.createCell(2).setCellValue(item.getValue() == null ? 0 : item.getValue());
            }
        }
        sheet.autoSizeColumn(1);
    }

    // 3. Sheet Sắp Hết Hàng
    private void createLowStockSheet(Workbook wb, List<AdProductResponse> data) {
        Sheet sheet = wb.createSheet("Sắp Hết");
        createHeader(wb, sheet, new String[]{"STT", "Tên Sản Phẩm", "Thương Hiệu", "Tồn Kho"});

        int rowIdx = 1;
        if (data != null) {
            for (AdProductResponse item : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(item.getName());
                row.createCell(2).setCellValue(item.getBrandName());

                Cell qty = row.createCell(3);
                qty.setCellValue(item.getQuantity());

                // Tô đỏ nếu hết hàng (số lượng = 0)
                if (item.getQuantity() == 0) {
                    CellStyle redStyle = wb.createCellStyle();
                    Font font = wb.createFont();
                    font.setColor(IndexedColors.RED.getIndex());
                    redStyle.setFont(font);
                    qty.setCellStyle(redStyle);
                }
            }
        }
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    // 4. Sheet Thương Hiệu
    private void createTopBrandsSheet(Workbook wb, List<AdChartResponse> data) {
        Sheet sheet = wb.createSheet("Thương Hiệu Hot");
        createHeader(wb, sheet, new String[]{"Hạng", "Thương Hiệu", "Số Sản Phẩm Bán"});

        int rowIdx = 1;
        if (data != null) {
            for (AdChartResponse item : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(item.getName());
                row.createCell(2).setCellValue(item.getValue());
            }
        }
        sheet.autoSizeColumn(1);
    }

    // 5. Sheet Đơn Hàng
    private void createOrdersSheet(Workbook wb, List<Object[]> data) {
        Sheet sheet = wb.createSheet("Đơn Hàng Mới");
        createHeader(wb, sheet, new String[]{"Mã ĐH", "Khách Hàng", "SĐT", "Tổng Tiền", "Ngày Tạo", "Trạng Thái"});

        CellStyle currency = createCurrencyStyle(wb);
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(wb.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

        int rowIdx = 1;
        if (data != null) {
            for (Object[] obj : data) {
                Row row = sheet.createRow(rowIdx++);
                // obj[0]: code, [1]: name, [2]: phone, [3]: amount, [4]: date, [5]: status
                row.createCell(0).setCellValue(obj[0] != null ? obj[0].toString() : "");
                row.createCell(1).setCellValue(obj[1] != null ? obj[1].toString() : "Khách lẻ");
                row.createCell(2).setCellValue(obj[2] != null ? obj[2].toString() : "");

                Cell money = row.createCell(3);
                money.setCellValue(obj[3] != null ? Double.parseDouble(obj[3].toString()) : 0);
                money.setCellStyle(currency);

                // Date
                try {
                    long ts = Long.parseLong(obj[4].toString());
                    Cell dt = row.createCell(4);
                    dt.setCellValue(new java.util.Date(ts));
                    dt.setCellStyle(dateStyle);
                } catch (Exception e) {}

                // Status Mapping
                String statusStr = "Khác";
                try {
                    int st = Integer.parseInt(obj[5].toString());
                    if (st == 0) statusStr = "Đã thanh toán";
                    else if (st == 1) statusStr = "Chờ xác nhận";
                    else if (st == 2) statusStr = "Đã hủy";
                    else if (st == 3) statusStr = "Đang xử lý";
                } catch (Exception e) {}
                row.createCell(5).setCellValue(statusStr);
            }
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(4);
    }

    // --- CÁC HÀM TIỆN ÍCH DÙNG CHUNG ---

    // Tạo Header Style (Nền xanh chữ trắng)
    private void createHeader(Workbook wb, Sheet sheet, String[] cols) {
        Row row = sheet.createRow(0);
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);

        for (int i = 0; i < cols.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(style);
        }
    }

    // Tạo Style tiền tệ (VNĐ)
    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0 ₫"));
        return style;
    }
}
