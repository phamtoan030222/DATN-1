package com.sd20201.datn.core.client.voucher.voucher.service.impl;

import com.sd20201.datn.core.admin.voucher.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.admin.voucher.voucher.model.response.AdVoucherResponse;
import com.sd20201.datn.core.admin.voucher.voucher.repository.AdVoucherRepository;
import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class ClientVoucherExportService {

    @Autowired
    private AdVoucherRepository voucherRepository;

    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("HH:mm dd/MM/yyyy");

    public void exportVouchersToExcel(HttpServletResponse response, AdVoucherRequest filter) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Danh sách Voucher");

            // Tạo Style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle centerStyle = createBorderedStyle(workbook);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            CellStyle leftStyle = createBorderedStyle(workbook);
            leftStyle.setAlignment(HorizontalAlignment.LEFT);
            CellStyle rightStyle = createBorderedStyle(workbook);
            rightStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Tạo Header
            String[] headers = {
                    "STT", "Mã Voucher", "Tên Voucher", "Loại", "Đối tượng",
                    "Giá trị giảm", "Giảm tối đa", "Đơn tối thiểu", "Số lượng", "Ghi chú",
                    "Bắt đầu", "Kết thúc", "Trạng thái"
            };
            int[] colWidths = {
                    2000,  // 0. STT (Nhỏ)
                    4500,  // 1. Mã Voucher
                    9000,  // 2. Tên Voucher (Cần rộng để hiển thị tên dài)
                    3500,  // 3. Loại
                    3500,  // 4. Đối tượng
                    4000,  // 5. Giá trị giảm
                    4000,  // 6. Giảm tối đa
                    4000,  // 7. Đơn tối thiểu
                    3000,  // 8. Số lượng
                    8000,  // 9. Ghi chú (Cần rộng)
                    5000,  // 10. Bắt đầu (Đủ hiển thị dd/MM/yyyy HH:mm)
                    5000,  // 11. Kết thúc
                    4500   // 12. Trạng thái
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);

                // Thiết lập độ rộng theo mảng đã định nghĩa
                if (i < colWidths.length) {
                    sheet.setColumnWidth(i, colWidths[i]);
                } else {
                    sheet.setColumnWidth(i, 4000); // Fallback nếu quên khai báo
                }
            }

            int page = 0;
            int rowIdx = 1;

            while (true) {
                // Gọi Repository để lấy dữ liệu (đã lọc theo period/status)
                Pageable pageable = PageRequest.of(page, 5000);
                Page<AdVoucherResponse> voucherPage = voucherRepository.getVouchers(pageable, filter);
                List<AdVoucherResponse> vouchers = voucherPage.getContent();

                if (vouchers.isEmpty()) break;

                for (AdVoucherResponse v : vouchers) {
                    Row row = sheet.createRow(rowIdx++);
                    createCell(row, 0, rowIdx - 1, centerStyle);
                    createCell(row, 1, v.getCode(), centerStyle);
                    createCell(row, 2, v.getName(), leftStyle);
                    createCell(row, 3, v.getTypeVoucher() == TypeVoucher.PERCENTAGE ? "Phần trăm" : "Tiền mặt", centerStyle);
                    createCell(row, 4, v.getTargetType() == TargetType.INDIVIDUAL ? "Cá nhân" : "Công khai", centerStyle);
                    createCell(row, 5, v.getTypeVoucher() == TypeVoucher.PERCENTAGE ? v.getDiscountValue() + "%" : formatMoney(v.getDiscountValue()), rightStyle);
                    createCell(row, 6, v.getMaxValue() != null ? formatMoney(v.getMaxValue()) : "-", rightStyle);
                    createCell(row, 7, formatMoney(v.getConditions()), rightStyle);
                    createCell(row, 8, v.getRemainingQuantity(), centerStyle);
                    createCell(row, 9, v.getNote(), leftStyle);
                    createCell(row, 10, formatDate(v.getStartDate()), centerStyle);
                    createCell(row, 11, formatDate(v.getEndDate()), centerStyle);
                    createCell(row, 12, getStatusText(v), centerStyle); // Tính toán trạng thái Sắp/Đang/Kết thúc
                }
                page++;
            }

            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            workbook.dispose();
            outputStream.close();
        }
    }

    // Logic hiển thị trạng thái trong Excel giống hệt Frontend
    private String getStatusText(AdVoucherResponse v) {
        long now = System.currentTimeMillis();
        long start = v.getStartDate() != null ? v.getStartDate() : 0;
        long end = v.getEndDate() != null ? v.getEndDate() : Long.MAX_VALUE;

        // 1. Sắp diễn ra
        if (start > now) return "Sắp diễn ra";

        // 2. Kiểm tra Kết thúc
        boolean isExpired = end < now;

        // SỬA Ở ĐÂY: Bỏ điều kiện check TargetType.
        // Dù là Công khai hay Cá nhân (Nhóm), cứ hết số lượng là Hết hàng.
        boolean isOutOfStock = v.getRemainingQuantity() != null && v.getRemainingQuantity() <= 0;

        if (isExpired || isOutOfStock) return "Đã kết thúc";


        if (v.getStatus() != null && v.getStatus() == 1) {
            return "Tạm dừng";
        }

        // 4. Còn lại
        return "Đang diễn ra";
    }

    private String formatMoney(Number n) {
        return n != null ? CURRENCY_FORMATTER.format(n) : "";
    }

    private String formatDate(Long t) {
        return t != null ? DATE_FORMATTER.format(new Date(t)) : "";
    }

    private void createCell(Row row, int col, Object val, CellStyle style) {
        Cell cell = row.createCell(col);
        if (val != null) cell.setCellValue(val.toString());
        else cell.setCellValue("");
        cell.setCellStyle(style);
    }

    // (Các hàm style giữ nguyên như cũ)
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle createBorderedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}