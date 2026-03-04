package com.sd20201.datn.utils;

import com.sd20201.datn.core.admin.shift.model.request.ExcelScheduleRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@Slf4j
public class ExcelHelper {

    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static byte[] createExcelStream(String sheetName, List<String> headers, List<List<Object>> data) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            int rowIndex = 0;

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setFontName("Times New Roman");
            font.setFontHeightInPoints((short) 12);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);
            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (List<Object> rowData : data) {
                Row row = sheet.createRow(rowIndex++);
                for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    Object value = rowData.get(colIndex);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value != null ? value.toString() : "");
                    }
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static boolean hasExcelFormat(MultipartFile file) {
        return TYPE.equals(file.getContentType());
    }

    // Đọc file Excel và chuyển thành List các Object Request
    public static List<ExcelScheduleRequest> excelToScheduleRequests(InputStream is) {
        try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên
            Iterator<Row> rows = sheet.iterator();
            List<ExcelScheduleRequest> listRequests = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                // Bỏ qua dòng Header (Dòng 0 là tiêu đề cột)
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Iterator<Cell> cellsInRow = currentRow.iterator();
                ExcelScheduleRequest request = new ExcelScheduleRequest();
                int cellIdx = 0;

                while (cellsInRow.hasNext()) {
                    Cell currentCell = cellsInRow.next();
                    switch (cellIdx) {
                        case 0: // Cột A: Mã nhân viên (Username)
                            request.setStaffCode(getCellValueAsString(currentCell));
                            break;
                        case 1: // Cột B: Ngày làm việc (dd/MM/yyyy)
                            request.setWorkDate(getCellValueAsString(currentCell));
                            break;
                        case 2: // Cột C: Tên ca làm việc
                            request.setShiftName(getCellValueAsString(currentCell));
                            break;
                        default:
                            break;
                    }
                    cellIdx++;
                }

                // Chỉ add những dòng có Mã nhân viên (tránh đọc nhầm các dòng trống ở cuối file Excel)
                if (request.getStaffCode() != null && !request.getStaffCode().trim().isEmpty()) {
                    listRequests.add(request);
                }
            }
            workbook.close();
            return listRequests;

        } catch (Exception e) {
            log.error("Lỗi khi đọc file Excel: ", e);
            throw new RuntimeException("Không thể đọc được file Excel: " + e.getMessage());
        }
    }

    // Hàm an toàn để lấy giá trị Cell dưới dạng String (Xử lý các loại format của Excel)
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();

        } else if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                // Nếu người dùng nhập chuẩn định dạng Date của Excel
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                return sdf.format(cell.getDateCellValue());
            } else {
                // Tránh việc Excel hiển thị số như 1.0 thay vì 1
                return String.valueOf((long) cell.getNumericCellValue());
            }
        }
        return "";
    }

}
