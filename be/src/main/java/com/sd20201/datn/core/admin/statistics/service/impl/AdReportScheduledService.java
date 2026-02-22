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
import com.sd20201.datn.core.admin.statistics.model.response.AdGrowthStatResponse;
import com.sd20201.datn.core.admin.statistics.service.AdStatisticsService;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class AdReportScheduledService {

    private final AdStatisticsService adStatisticsService;
    private final ADStaffRepository staffRepo;
    private final JavaMailSender mailSender;

    @Scheduled(cron = "0 0 22 * * *")
    public void runAutoReporting() {
        // Có thể thêm logic kiểm tra thứ/ngày tháng tại đây như code cũ
        executeReporting("Ngay", "ngay");
    }

    public void executeReporting(String title, String keyword) {
        try {
            List<String> managerEmails = staffRepo.findEmailsByRoleAndStatus(RoleConstant.QUAN_LY, EntityStatus.ACTIVE);
            if (managerEmails.isEmpty()) return;

            // 1. Lấy dữ liệu tổng quan
            AdDashboardOverviewResponse overview = adStatisticsService.getDashboardOverview();

            // 2. Lấy dữ liệu tăng trưởng
            List<AdGrowthStatResponse> growthStats = adStatisticsService.getGrowthStatistics();

            // 3. [MỚI] Tính toán thời gian và lấy Top Sản phẩm theo kỳ báo cáo
            long startMs;
            long endMs = System.currentTimeMillis();
            LocalDateTime now = LocalDateTime.now();

            if (title.equalsIgnoreCase("Ngay")) {
                startMs = toMs(now.toLocalDate().atStartOfDay());
            } else if (title.equalsIgnoreCase("Tuan")) {
                startMs = toMs(now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay());
            } else if (title.equalsIgnoreCase("Thang")) {
                startMs = toMs(now.withDayOfMonth(1).toLocalDate().atStartOfDay());
            } else { // Năm
                startMs = toMs(now.withDayOfYear(1).toLocalDate().atStartOfDay());
            }

            // Gọi hàm mới để lấy Top 5 chính xác trong khoảng thời gian này
            List<AdDashboardOverviewResponse.TopItemDTO> topProducts = adStatisticsService.getTopSellingProductsByDateRange(startMs, endMs);

            byte[] pdfContent = createProfessionalPdfNoAccent(title, keyword, overview, growthStats, topProducts);

            for (String email : managerEmails) {
                sendMail(email, title, pdfContent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] createProfessionalPdfNoAccent(
            String title,
            String keyword,
            AdDashboardOverviewResponse overview,
            List<AdGrowthStatResponse> growthStats,
            List<AdDashboardOverviewResponse.TopItemDTO> topProducts // Nhận list mới làm tham số
    ) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        // Header
        doc.add(new Paragraph("BAO CAO THONG KE DOANH THU DINH KY")
                .setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER).setFontColor(ColorConstants.BLUE));
        doc.add(new Paragraph("Loai bao cao: " + title).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Thoi gian xuat: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .setFontSize(10).setTextAlignment(TextAlignment.RIGHT));

        // 1. TĂNG TRƯỞNG
        doc.add(new Paragraph("\n1. CHI SO TANG TRUONG SO VOI KY TRUOC:").setBold());
        Table growthTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 15, 15})).useAllAvailableWidth();
        growthTable.addHeaderCell(new Cell().add(new Paragraph("Chi so")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        growthTable.addHeaderCell(new Cell().add(new Paragraph("Gia tri")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        growthTable.addHeaderCell(new Cell().add(new Paragraph("%")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        growthTable.addHeaderCell(new Cell().add(new Paragraph("Trend")).setBackgroundColor(ColorConstants.LIGHT_GRAY));

        for (AdGrowthStatResponse stat : growthStats) {
            if (stat.getLabel().toLowerCase().contains(keyword.toLowerCase())) {
                growthTable.addCell(stat.getLabel().replace("đ", "d").replace("ê", "e"));
                growthTable.addCell(stat.getValue());
                growthTable.addCell(stat.getPercent() + "%");
                Cell trendCell = new Cell().add(new Paragraph(stat.isIncrease() ? "TANG" : "GIAM"));
                trendCell.setFontColor(stat.isIncrease() ? ColorConstants.GREEN : ColorConstants.RED);
                growthTable.addCell(trendCell);
            }
        }
        doc.add(growthTable);

        // 2. CHI TIẾT VẬN HÀNH
        doc.add(new Paragraph("\n2. CHI TIET VAN HANH KY NAY:").setBold());
        AdDashboardOverviewResponse.StatisticCard card = overview.getToday();
        if(title.equalsIgnoreCase("Tuan")) card = overview.getWeek();
        else if(title.equalsIgnoreCase("Thang")) card = overview.getMonth();
        else if(title.equalsIgnoreCase("Nam")) card = overview.getYear();

        Table detailTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        detailTable.addCell("Tong don hang:"); detailTable.addCell(String.valueOf(card.getTotalOrders()));
        detailTable.addCell("Don hoan thanh:"); detailTable.addCell(String.valueOf(card.getCompleted()));
        detailTable.addCell("Don da huy:"); detailTable.addCell(String.valueOf(card.getCancelled()));
        detailTable.addCell("Don dang xu ly:"); detailTable.addCell(String.valueOf(card.getProcessing()));
        detailTable.addCell("San pham da ban:"); detailTable.addCell(String.valueOf(card.getSoldProducts()));
        detailTable.addCell("Doanh thu thuc te:"); detailTable.addCell(String.format("%,d VND", card.getRevenue()));
        detailTable.addCell("Doanh thu du kien:"); detailTable.addCell(String.format("%,d VND", card.getExpectedRevenue()));
        doc.add(detailTable);

        // 3. TOP 5 SẢN PHẨM (Dùng list topProducts truyền vào thay vì overview)
        doc.add(new Paragraph("\n3. TOP 5 SAN PHAM BAN CHAY TRONG " + title.toUpperCase() + ":").setBold());
        Table topTable = new Table(UnitValue.createPercentArray(new float[]{10, 60, 30})).useAllAvailableWidth();
        topTable.addHeaderCell("#");
        topTable.addHeaderCell("Ten san pham");
        topTable.addHeaderCell("So luong");

        int rank = 1;
        if (topProducts == null || topProducts.isEmpty()) {
            topTable.addCell("-"); topTable.addCell("Khong co du lieu ban hang"); topTable.addCell("0");
        } else {
            for (AdDashboardOverviewResponse.TopItemDTO item : topProducts) {
                topTable.addCell(String.valueOf(rank++));
                topTable.addCell(item.getName());
                topTable.addCell(String.valueOf(item.getCount()));
                if (rank > 5) break;
            }
        }
        doc.add(topTable);

        doc.close();
        return baos.toByteArray();
    }

    private void sendMail(String to, String title, byte[] pdf) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject("[My LapTop] Bao cao " + title);
        helper.setText("Kinh gui Quan ly,\nHe thong gui ban bao cao thong ke chi tiet ngay " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        helper.addAttachment("BaoCao_ThongKe_" + title + ".pdf", new org.springframework.core.io.ByteArrayResource(pdf));
        mailSender.send(message);
    }

    private Long toMs(LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}