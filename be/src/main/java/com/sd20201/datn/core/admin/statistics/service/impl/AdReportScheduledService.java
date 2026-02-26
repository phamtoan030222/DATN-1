package com.sd20201.datn.core.admin.statistics.service.impl;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.sd20201.datn.core.admin.staff.repository.ADStaffRepository;
import com.sd20201.datn.core.admin.statistics.model.response.AdDashboardOverviewResponse;
import com.sd20201.datn.core.admin.statistics.model.response.AdProductResponse;
import com.sd20201.datn.core.admin.statistics.model.response.AdRevenueChartResponse;
import com.sd20201.datn.core.admin.statistics.repository.AdStatisticsRepository;
import com.sd20201.datn.core.admin.statistics.service.AdStatisticsService;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdReportScheduledService {

    private final AdStatisticsService adStatisticsService;
    private final AdStatisticsRepository statisticsRepo;
    private final ADStaffRepository staffRepo;
    private final JavaMailSender mailSender;

    @Scheduled(cron = "0 0 22 * * *")
    public void runAutoReporting() {
        LocalDateTime now = LocalDateTime.now();

        executeReporting("Ngay");
        if (now.getDayOfWeek().getValue() == 7) executeReporting("Tuan");

        if (now.toLocalDate().equals(now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth()))) {
            executeReporting("Thang");
            // Cuối tháng 3, 6, 9, 12 thì chạy thêm báo cáo Quý
            if (now.getMonthValue() % 3 == 0) executeReporting("Quy");
        }

        if (now.getMonthValue() == 12 && now.getDayOfMonth() == 31) executeReporting("Nam");
    }

    public void executeReporting(String title) {
        try {
            List<String> managerEmails = staffRepo.findEmailsByRoleAndStatus(RoleConstant.QUAN_LY, EntityStatus.ACTIVE);
            if (managerEmails.isEmpty()) return;

            LocalDateTime now = LocalDateTime.now();
            long startMs, endMs = toMs(now);
            long prevStartMs, prevEndMs;
            String chartType = "month";

            // 1. TÍNH TOÁN KỲ HIỆN TẠI VÀ KỲ TRƯỚC (ĐỂ SO SÁNH)
            if (title.equalsIgnoreCase("Ngay")) {
                startMs = toMs(now.toLocalDate().atStartOfDay());
                prevStartMs = toMs(now.minusDays(1).toLocalDate().atStartOfDay());
                prevEndMs = toMs(now.minusDays(1).toLocalDate().atTime(23, 59, 59));
                chartType = "today";
            } else if (title.equalsIgnoreCase("Tuan")) {
                startMs = toMs(now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay());
                prevStartMs = toMs(now.minusWeeks(1).with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay());
                prevEndMs = toMs(now.minusWeeks(1).with(DayOfWeek.SUNDAY).toLocalDate().atTime(23, 59, 59));
                chartType = "week";
            } else if (title.equalsIgnoreCase("Thang")) {
                startMs = toMs(now.withDayOfMonth(1).toLocalDate().atStartOfDay());
                prevStartMs = toMs(now.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay());
                prevEndMs = toMs(now.minusMonths(1).withDayOfMonth(now.minusMonths(1).toLocalDate().lengthOfMonth()).toLocalDate().atTime(23, 59, 59));
                chartType = "month";
            } else if (title.equalsIgnoreCase("Quy")) {
                // 1. Xác định Quý hiện tại (Ví dụ: đang tháng 2 -> Quý 1: T1, T2, T3)
                int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int startMonth = (currentQuarter - 1) * 3 + 1;

                LocalDateTime quarterStart = now.withMonth(startMonth).withDayOfMonth(1).toLocalDate().atStartOfDay();

                LocalDateTime quarterEnd = quarterStart.plusMonths(2).withDayOfMonth(quarterStart.plusMonths(2).toLocalDate().lengthOfMonth()).toLocalDate().atTime(23, 59, 59);

                startMs = toMs(quarterStart);
                endMs = toMs(quarterEnd);

                // 2. SO SÁNH VỚI CÙNG KỲ NĂM TRƯỚC (Year-over-Year)
                prevStartMs = toMs(quarterStart.minusYears(1));
                prevEndMs = toMs(quarterEnd.minusYears(1));

                chartType = "year";
            } else { // Năm
                startMs = toMs(now.withDayOfYear(1).toLocalDate().atStartOfDay());
                prevStartMs = toMs(now.minusYears(1).withDayOfYear(1).toLocalDate().atStartOfDay());
                prevEndMs = toMs(now.minusYears(1).withDayOfYear(now.minusYears(1).toLocalDate().lengthOfYear()).toLocalDate().atTime(23, 59, 59));
                chartType = "year";
            }

            // 2. LẤY DỮ LIỆU TỪ DATABASE
            Map<String, Object> currentStats = statisticsRepo.getStatsByPeriod(startMs, endMs);
            Map<String, Object> prevStats = statisticsRepo.getStatsByPeriod(prevStartMs, prevEndMs);

            List<AdDashboardOverviewResponse.TopItemDTO> topProducts = adStatisticsService.getTopSellingProductsByDateRange(startMs, endMs);
            Page<AdProductResponse> lowStockPage = adStatisticsService.getLowStockProducts(10, org.springframework.data.domain.PageRequest.of(0, 10));
            AdRevenueChartResponse chartData = adStatisticsService.getRevenueChart(chartType, startMs, endMs);

            // 3. TẠO PDF VÀ GỬI MAIL
            byte[] pdfContent = createProfessionalPdfNoAccent(title, currentStats, prevStats, topProducts, lowStockPage, chartData);

            for (String email : managerEmails) {
                sendHtmlMail(email, title, pdfContent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] createProfessionalPdfNoAccent(
            String title,
            Map<String, Object> currentStats,
            Map<String, Object> prevStats,
            List<AdDashboardOverviewResponse.TopItemDTO> topProducts,
            Page<AdProductResponse> lowStockPage,
            AdRevenueChartResponse chartData
    ) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        doc.add(new Paragraph("BAO CAO THONG KE DOANH THU DINH KY")
                .setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER).setFontColor(ColorConstants.BLUE));
        doc.add(new Paragraph("Loai bao cao: " + title.toUpperCase()).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Thoi gian xuat: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .setFontSize(10).setTextAlignment(TextAlignment.RIGHT));

        // ================= 1. SO SÁNH TĂNG TRƯỞNG =================
        String compareText = (title.equalsIgnoreCase("Quy") || title.equalsIgnoreCase("Nam")) ? "CUNG KY NAM TRUOC" : "KY TRUOC";
        doc.add(new Paragraph("\n1. CHI SO TANG TRUONG SO VOI " + compareText + ":").setBold());
        Table growthTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 15, 15})).useAllAvailableWidth();
        growthTable.addHeaderCell(new Cell().add(new Paragraph("Chi so")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        growthTable.addHeaderCell(new Cell().add(new Paragraph("Gia tri")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        growthTable.addHeaderCell(new Cell().add(new Paragraph("%")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        growthTable.addHeaderCell(new Cell().add(new Paragraph("Trend")).setBackgroundColor(ColorConstants.LIGHT_GRAY));

        long curRev = toLong(currentStats.get("revenue"));
        long prevRev = toLong(prevStats.get("revenue"));
        addGrowthRow(growthTable, "Doanh thu", String.format("%,d VND", curRev), calculateGrowth(curRev, prevRev));

        long curOrders = toLong(currentStats.get("totalOrders"));
        long prevOrders = toLong(prevStats.get("totalOrders"));
        addGrowthRow(growthTable, "Don hang", String.valueOf(curOrders), calculateGrowth(curOrders, prevOrders));

        long curProd = toLong(currentStats.get("soldProducts"));
        long prevProd = toLong(prevStats.get("soldProducts"));
        addGrowthRow(growthTable, "San pham da ban", String.valueOf(curProd), calculateGrowth(curProd, prevProd));

        doc.add(growthTable);

        // ================= 2. CHI TIẾT VẬN HÀNH =================
        doc.add(new Paragraph("\n2. CHI TIET VAN HANH KY NAY:").setBold());
        Table detailTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        detailTable.addCell("Tong don hang:"); detailTable.addCell(String.valueOf(curOrders));
        detailTable.addCell("Don hoan thanh:"); detailTable.addCell(String.valueOf(toLong(currentStats.get("completed"))));
        detailTable.addCell("Don da huy:"); detailTable.addCell(String.valueOf(toLong(currentStats.get("cancelled"))));
        detailTable.addCell("Don dang xu ly:"); detailTable.addCell(String.valueOf(toLong(currentStats.get("processing"))));
        detailTable.addCell("San pham da ban:"); detailTable.addCell(String.valueOf(curProd));
        detailTable.addCell("Doanh thu thuc te:"); detailTable.addCell(String.format("%,d VND", curRev));
        detailTable.addCell("Doanh thu du kien:"); detailTable.addCell(String.format("%,d VND", toLong(currentStats.get("expectedRevenue"))));
        doc.add(detailTable);

        // ================= 3. CHI TIẾT DOANH THU THEO THỜI GIAN =================
        doc.add(new Paragraph("\n3. CHI TIET DOANH THU THEO THOI GIAN (" + title.toUpperCase() + "):").setBold());
        Table timeTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        timeTable.addHeaderCell(new Cell().add(new Paragraph("Thoi gian")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        timeTable.addHeaderCell(new Cell().add(new Paragraph("Doanh thu")).setBackgroundColor(ColorConstants.LIGHT_GRAY));

        if (chartData != null && chartData.getTimeLabels() != null) {
            for (int i = 0; i < chartData.getTimeLabels().size(); i++) {
                String label = chartData.getTimeLabels().get(i);

                // NẾU LÀ QUÝ: Lọc để chỉ in ra 3 tháng thuộc Quý đó
                if (title.equalsIgnoreCase("Quy")) {
                    try {
                        int monthNum = Integer.parseInt(label.replaceAll("[^0-9]", ""));
                        int quarterOfLabel = (monthNum - 1) / 3 + 1;
                        int currentQuarter = (LocalDateTime.now().getMonthValue() - 1) / 3 + 1;
                        if (quarterOfLabel != currentQuarter) continue; // Bỏ qua nếu không cùng Quý
                    } catch (Exception e) {}
                }

                timeTable.addCell(label.replace("Thứ", "Thu").replace("CN", "Chu Nhat"));
                timeTable.addCell(String.format("%,d VND", chartData.getCurrentData().get(i)));
            }
        }
        doc.add(timeTable);

        // ================= 4. TOP 5 SẢN PHẨM =================
        doc.add(new Paragraph("\n4. TOP 5 SAN PHAM BAN CHAY TRONG " + title.toUpperCase() + ":").setBold());
        Table topTable = new Table(UnitValue.createPercentArray(new float[]{10, 60, 30})).useAllAvailableWidth();
        topTable.addHeaderCell("#"); topTable.addHeaderCell("Ten san pham"); topTable.addHeaderCell("So luong");

        if (topProducts == null || topProducts.isEmpty()) {
            topTable.addCell("-"); topTable.addCell("Khong co du lieu ban hang"); topTable.addCell("0");
        } else {
            int rank = 1;
            for (AdDashboardOverviewResponse.TopItemDTO item : topProducts) {
                topTable.addCell(String.valueOf(rank++));
                topTable.addCell(item.getName());
                topTable.addCell(String.valueOf(item.getCount()));
                if (rank > 5) break;
            }
        }
        doc.add(topTable);

        // ================= 5. CẢNH BÁO SẮP HẾT HÀNG =================
        doc.add(new Paragraph("\n5. CANH BAO SAN PHAM SAP HET HANG (Ton kho <= 10):")
                .setBold().setFontColor(ColorConstants.RED));
        Table lowStockTable = new Table(UnitValue.createPercentArray(new float[]{10, 50, 25, 15})).useAllAvailableWidth();
        lowStockTable.addHeaderCell("#"); lowStockTable.addHeaderCell("Ten san pham");
        lowStockTable.addHeaderCell("Thuong hieu"); lowStockTable.addHeaderCell("Ton kho");

        List<AdProductResponse> lowStockList = lowStockPage.getContent();
        if (lowStockList.isEmpty()) {
            lowStockTable.addCell("-"); lowStockTable.addCell("Kho khong co san pham sap het."); lowStockTable.addCell("-"); lowStockTable.addCell("-");
        } else {
            int r = 1;
            for (AdProductResponse prod : lowStockList) {
                lowStockTable.addCell(String.valueOf(r++));
                lowStockTable.addCell(prod.getName() != null ? prod.getName() : "N/A");
                lowStockTable.addCell(prod.getBrandName() != null ? prod.getBrandName() : "N/A");
                Cell qtyCell = new Cell().add(new Paragraph(String.valueOf(prod.getQuantity())));
                qtyCell.setFontColor(ColorConstants.RED).setBold();
                lowStockTable.addCell(qtyCell);
            }
        }
        doc.add(lowStockTable);

        doc.close();
        return baos.toByteArray();
    }

    // Các hàm Helper hỗ trợ tính toán
    private void addGrowthRow(Table table, String label, String value, double percent) {
        table.addCell(label);
        table.addCell(value);
        table.addCell(String.format("%.1f%%", Math.abs(percent)));
        Cell trend = new Cell().add(new Paragraph(percent >= 0 ? "TANG" : "GIAM"));
        trend.setFontColor(percent >= 0 ? ColorConstants.GREEN : ColorConstants.RED);
        table.addCell(trend);
    }

    private double calculateGrowth(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return (double) (current - previous) / previous * 100;
    }

    private Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return 0L; }
    }

    private Long toMs(LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ================= EMAIL GIAO DIỆN HTML (Màu Xanh Build My Pc) =================
    private void sendHtmlMail(String to, String title, byte[] pdf) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String vnTitle = switch (title.toLowerCase()) {
            case "ngay" -> "Ngày"; case "tuan" -> "Tuần"; case "thang" -> "Tháng"; case "quy" -> "Quý"; case "nam" -> "Năm"; default -> title;
        };

        helper.setTo(to);
        helper.setSubject("[My LapTop] Báo cáo thống kê doanh thu - " + vnTitle);

        String htmlMsg = "<div style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #34C759; border-radius: 8px; overflow: hidden;'>"
                + "<div style='background-color: #34C759; color: #ffffff; padding: 20px; text-align: center;'>"
                + "<h2 style='margin: 0;'>BÁO CÁO VẬN HÀNH ĐỊNH KỲ</h2>"
                + "<p style='margin: 5px 0 0; font-size: 14px;'>Hệ thống quản lý cửa hàng My LapTop</p>"
                + "</div>"
                + "<div style='padding: 20px;'>"
                + "<p><strong>Kính gửi Quản lý,</strong></p>"
                + "<p>Hệ thống tự động gửi đến bạn báo cáo thống kê chi tiết cho kỳ <strong>" + vnTitle + "</strong>.</p>"
                + "<p>Vui lòng xem file PDF đính kèm để nắm bắt các thông tin quan trọng:</p>"
                + "<ul style='color: #34C759;'>"
                + "<li><span style='color: #333'>Tốc độ tăng trưởng Doanh thu & Đơn hàng.</span></li>"
                + "<li><span style='color: #333'>Chi tiết phân bổ doanh thu theo thời gian.</span></li>"
                + "<li><span style='color: #333'>Top 5 sản phẩm bán chạy nhất kỳ này.</span></li>"
                + "<li><strong style='color: #d03050;'>Cảnh báo các sản phẩm sắp hết hàng trong kho.</strong></li>"
                + "</ul>"
                + "<p>Chúc bạn một ngày làm việc hiệu quả!</p>"
                + "</div>"
                + "<div style='background-color: #f9f9f9; color: #888; padding: 15px; text-align: center; font-size: 12px; border-top: 1px solid #e0e0e0;'>"
                + "<p style='margin: 0;'>Đây là email tự động từ hệ thống, vui lòng không trả lời.</p>"
                + "</div></div>";

        helper.setText(htmlMsg, true);
        String fileName = "BaoCao_" + title + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".pdf";
        helper.addAttachment(fileName, new org.springframework.core.io.ByteArrayResource(pdf));

        mailSender.send(message);
    }
}