package com.sd20201.datn.core.admin.shift.service.impl;

import com.sd20201.datn.core.admin.shift.model.request.BulkCreateScheduleRequest;
import com.sd20201.datn.core.admin.shift.model.request.CreateScheduleRequest;
import com.sd20201.datn.core.admin.shift.repository.AdWorkScheduleRepository;
import com.sd20201.datn.core.admin.shift.service.WorkScheduleService;
import com.sd20201.datn.core.admin.staff.repository.ADStaffRepository;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.Shift;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.entity.WorkSchedule;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.repository.AccountRepository;
import com.sd20201.datn.repository.ShiftRepository;
import com.sd20201.datn.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.sd20201.datn.utils.ExcelHelper;
import com.sd20201.datn.core.admin.shift.model.request.ExcelScheduleRequest;
import org.springframework.web.multipart.MultipartFile;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;

@Service
public class WorkScheduleServiceImpl implements WorkScheduleService {
    @Autowired
    private AdWorkScheduleRepository scheduleRepo;

    @Autowired
    private ADStaffRepository staffRepo; // ✅ Sửa thành StaffRepository

    @Autowired
    private ShiftRepository shiftTemplateRepo;

    // Trong WorkScheduleServiceImpl.java

    @Override
    public ResponseObject<?> createSchedule(CreateScheduleRequest req) {
        // 1. Tìm nhân viên & Ca (Giữ nguyên code cũ của bạn)
        Staff staff = staffRepo.findById(req.getStaffId().trim())
                .orElseThrow(() -> new RuntimeException("Nhân viên không tồn tại"));
        Shift shift = shiftTemplateRepo.findById(req.getShiftId().trim())
                .orElseThrow(() -> new RuntimeException("Ca làm việc không tồn tại"));

        // 👇 SỬA LOGIC: Kiểm tra xem Ca này + Ngày này đã có ai làm chưa?
        // Nếu có rồi -> Cập nhật người mới. Nếu chưa -> Tạo mới.
        // (Tránh việc 1 ca có 2 người hoặc sửa xong không thấy đổi)

        WorkSchedule ws = scheduleRepo.findByShiftIdAndWorkDate(shift.getId(), req.getWorkDate());

        if (ws == null) {
            // Chưa có ai -> Tạo mới
            ws = new WorkSchedule();
            ws.setShift(shift);
            ws.setWorkDate(req.getWorkDate());
        }

        // Cập nhật nhân viên mới
        ws.setStaff(staff);
        ws.setNote(req.getNote());

        scheduleRepo.save(ws); // Lưu lại

        return new ResponseObject<>(ws, HttpStatus.OK, "Xếp lịch thành công");
    }

    // 👇 THÊM HÀM XÓA
    @Override
    public void deleteSchedule(String id) {
        scheduleRepo.deleteById(id);
    }

    @Override
    public ResponseObject<?> getSchedules(LocalDate fromDate, LocalDate toDate) {
        List<WorkSchedule> list = scheduleRepo.findByWorkDateBetween(fromDate, toDate);
        return new ResponseObject<>(list, HttpStatus.OK, "Lấy dữ liệu thành công");
    }

    @Override
    @Transactional
    public ResponseObject<?> createBulkSchedule(BulkCreateScheduleRequest req) {
        Staff staff = staffRepo.findById(req.getStaffId().trim())
                .orElseThrow(() -> new RuntimeException("Nhân viên không tồn tại"));

        List<Shift> selectedShifts = shiftTemplateRepo.findAllById(req.getShiftIds());
        if (selectedShifts.isEmpty()) throw new RuntimeException("Chưa chọn ca làm việc nào");

        LocalDateTime now = LocalDateTime.now();
        int successCount = 0;
        int errorCount = 0;

        // Lặp qua từng ngày trong khoảng thời gian đã chọn
        for (LocalDate date = req.getStartDate(); !date.isAfter(req.getEndDate()); date = date.plusDays(1)) {

            // 1. Kiểm tra Ngày lặp lại (2-4-6 hoặc 3-5-7)
            if (req.getDaysOfWeek() != null && !req.getDaysOfWeek().contains(date.getDayOfWeek().getValue())) {
                continue;
            }

            // Lấy danh sách lịch hiện có của NV này trong ngày để check giao cắt thời gian
            List<WorkSchedule> staffDailySchedules = scheduleRepo.findByStaffIdAndWorkDate(staff.getId(), date);

            for (Shift shift : selectedShifts) {
                // Chuẩn hóa giờ "08:00" thành "08:00:00" để parse
                LocalTime startTime = LocalTime.parse(shift.getStartTime().length() > 5 ? shift.getStartTime() : shift.getStartTime() + ":00");
                LocalTime endTime = LocalTime.parse(shift.getEndTime().length() > 5 ? shift.getEndTime() : shift.getEndTime() + ":00");
                LocalDateTime shiftStartDateTime = LocalDateTime.of(date, startTime);

                // CHỐT CHẶN 1: Bỏ qua nếu thời gian bắt đầu ca đã ở trong quá khứ
                if (shiftStartDateTime.isBefore(now)) {
                    errorCount++; continue;
                }

                // CHỐT CHẶN 2: Bỏ qua nếu ca này ngày này đã có NHÂN VIÊN KHÁC làm (Quy tắc 1 NV/1 Ca)
                WorkSchedule existingSchedule = scheduleRepo.findByShiftIdAndWorkDate(shift.getId(), date);
                if (existingSchedule != null && !existingSchedule.getStaff().getId().equals(staff.getId())) {
                    // 👇 KIỂM TRA QUYỀN GHI ĐÈ Ở ĐÂY
                    if (req.getOverwrite() == null || !req.getOverwrite()) {
                        errorCount++; continue;
                    }
                    // Nếu có quyền ghi đè (isOverwrite = true) -> Cho phép code chạy tiếp xuống dưới để cập nhật nhân viên mới
                }

                // CHỐT CHẶN 3: Kiểm tra TRÙNG GIỜ với các ca khác của CÙNG 1 nhân viên
                boolean isOverlap = false;
                for (WorkSchedule ws : staffDailySchedules) {
                    if (ws.getShift().getId().equals(shift.getId())) continue; // Cùng 1 ca thì bỏ qua

                    LocalTime wsStart = LocalTime.parse(ws.getShift().getStartTime().length() > 5 ? ws.getShift().getStartTime() : ws.getShift().getStartTime() + ":00");
                    LocalTime wsEnd = LocalTime.parse(ws.getShift().getEndTime().length() > 5 ? ws.getShift().getEndTime() : ws.getShift().getEndTime() + ":00");

                    // Logic giao cắt: Bắt đầu ca 1 < Kết thúc ca 2 VÀ Kết thúc ca 1 > Bắt đầu ca 2
                    if (startTime.isBefore(wsEnd) && endTime.isAfter(wsStart)) {
                        isOverlap = true; break;
                    }
                }

                if (isOverlap) {
                    errorCount++; continue;
                }

                // Nếu vượt qua mọi chốt chặn -> Lưu hoặc Cập nhật
                if (existingSchedule == null) {
                    existingSchedule = new WorkSchedule();
                    existingSchedule.setShift(shift);
                    existingSchedule.setWorkDate(date);
                }
                existingSchedule.setStaff(staff);
                scheduleRepo.save(existingSchedule);

                // Thêm vào list tạm để các vòng lặp shift sau trong cùng 1 ngày có thể check overlap
                staffDailySchedules.add(existingSchedule);
                successCount++;
            }
        }

        String msg = "Đã xếp thành công " + successCount + " ca làm việc.";
        if (errorCount > 0) {
            msg += " Đã bỏ qua " + errorCount + " ca vi phạm quy tắc (quá khứ, trùng giờ, hoặc đã có người làm).";
        }
        return new ResponseObject<>(null, HttpStatus.OK, msg);
    }

    @Override
    @Transactional // Rất quan trọng: Nếu 1 dòng lỗi, sẽ rollback không lưu bất kỳ dòng nào
    public ResponseObject<?> importExcelSchedule(MultipartFile file) {
        // 1. Kiểm tra định dạng file
        if (!ExcelHelper.hasExcelFormat(file)) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Vui lòng upload file Excel (.xlsx)");
        }

        try {
            // 2. Đọc file lấy danh sách dữ liệu (Dùng ExcelHelper)
            List<ExcelScheduleRequest> requests = ExcelHelper.excelToScheduleRequests(file.getInputStream());
            List<WorkSchedule> schedulesToSave = new ArrayList<>();
            // Dùng d/M/yyyy giúp Java đọc được cả "05/03/2026" lẫn "5/3/2026"
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/M/yyyy");

            if (requests.isEmpty()) {
                return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "File Excel trống hoặc không có dữ liệu hợp lệ.");
            }

            int rowNum = 2; // Bắt đầu tính từ dòng 2 (Dòng 1 là Header)
            int successCount = 0;

            // 3. Lặp qua từng dòng để Validate và Map sang Entity
            for (ExcelScheduleRequest req : requests) {

                // --- VALIDATE NHÂN VIÊN ---
                // Sửa lại theo tên hàm thực tế trong StaffRepository của bạn (tìm theo Mã NV hoặc Username)
                // Giả sử bạn đang tìm theo Code (Ví dụ: NV001)
                Optional<Staff> optStaff = staffRepo.findByCode(req.getStaffCode().trim());
                // Nếu staffRepo chưa có findByCode, bạn phải tự tạo trong Repository nhé.
                if (optStaff.isEmpty()) {
                    throw new RuntimeException("Lỗi dòng " + rowNum + ": Không tìm thấy nhân viên có mã '" + req.getStaffCode() + "'");
                }
                Staff staff = optStaff.get();

                // --- VALIDATE CA LÀM VIỆC ---
                Shift shift = shiftTemplateRepo.findByName(req.getShiftName().trim());
                if (shift == null) {
                    throw new RuntimeException("Lỗi dòng " + rowNum + ": Không tồn tại ca làm việc tên '" + req.getShiftName() + "'");
                }

                // --- VALIDATE NGÀY LÀM VIỆC ---
                LocalDate workDate;
                try {
                    workDate = LocalDate.parse(req.getWorkDate().trim(), dateFormatter);
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi dòng " + rowNum + ": Ngày làm việc sai định dạng dd/MM/yyyy (" + req.getWorkDate() + ")");
                }

                // --- KIỂM TRA TRÙNG LẶP ---
                // Kiểm tra xem ca này, ngày này đã có ai làm chưa?
                WorkSchedule existingSchedule = scheduleRepo.findByShiftIdAndWorkDate(shift.getId(), workDate);

                if (existingSchedule != null) {
                    // Tùy logic dự án:
                    // Cách 1: Báo lỗi không cho import
                    // throw new RuntimeException("Lỗi dòng " + rowNum + ": Ca '" + shift.getName() + "' ngày " + workDate + " đã được phân công cho người khác.");

                    // Cách 2: Ghi đè người mới (Sử dụng cách này cho giống logic createSchedule cũ của bạn)
                    existingSchedule.setStaff(staff);
                    schedulesToSave.add(existingSchedule);
                } else {
                    // Tạo mới
                    WorkSchedule newSchedule = new WorkSchedule();
                    newSchedule.setShift(shift);
                    newSchedule.setWorkDate(workDate);
                    newSchedule.setStaff(staff);
                    // Giả sử bạn có EntityStatus, nếu có hãy set: newSchedule.setStatus(EntityStatus.ACTIVE);

                    schedulesToSave.add(newSchedule);
                }

                rowNum++;
                successCount++;
            }

            // 4. Lưu tất cả vào Database
            if (!schedulesToSave.isEmpty()) {
                scheduleRepo.saveAll(schedulesToSave);
            }

            return new ResponseObject<>(null, HttpStatus.OK, "Đã import thành công " + successCount + " dòng dữ liệu xếp lịch!");

        } catch (RuntimeException e) {
            // Bắt lỗi Validation (sai mã NV, sai ca...) và gửi về FE
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // Bắt lỗi hệ thống
            e.printStackTrace();
            return new ResponseObject<>(null, HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống khi đọc file Excel.");
        }
    }

    @Override
    public byte[] downloadTemplate() {
        try {
            // 1. Lấy danh sách Mã NV và Tên Ca từ Database (CHỈ LẤY ACTIVE)
            List<String> staffCodes = staffRepo.findByStatus(EntityStatus.ACTIVE).stream()
                    .map(Staff::getCode)
                    .filter(code -> code != null && !code.isEmpty())
                    .collect(Collectors.toList());

            List<String> shiftNames = shiftTemplateRepo.findByStatus(EntityStatus.ACTIVE).stream()
                    .map(Shift::getName)
                    .filter(name -> name != null && !name.isEmpty())
                    .collect(Collectors.toList());

            // Đề phòng trường hợp DB rỗng
            if (staffCodes.isEmpty()) staffCodes.add("CHUA_CO_NV_ACTIVE");
            if (shiftNames.isEmpty()) shiftNames.add("CHUA_CO_CA_ACTIVE");

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet mainSheet = workbook.createSheet("Xep_Lich");
                Sheet hiddenSheet = workbook.createSheet("HiddenData");

                // --- 2. TẠO SHEET ẨN (Chứa data cho Dropdown) ---
                for (int i = 0; i < Math.max(staffCodes.size(), shiftNames.size()); i++) {
                    Row row = hiddenSheet.createRow(i);
                    if (i < staffCodes.size()) row.createCell(0).setCellValue(staffCodes.get(i));
                    if (i < shiftNames.size()) row.createCell(1).setCellValue(shiftNames.get(i));
                }
                // Ẩn sheet này đi
                workbook.setSheetHidden(1, true);

                // ==========================================
                // --- 3. TẠO HEADER (DÒNG ĐẦU TIÊN - INDEX 0) ---
                // ==========================================
                Row headerRow = mainSheet.createRow(0);
                headerRow.setHeightInPoints(25); // Cho dòng cao lên xíu
                headerRow.createCell(0).setCellValue("Mã nhân viên");
                headerRow.createCell(1).setCellValue("Ngày làm (dd/MM/yyyy)");
                headerRow.createCell(2).setCellValue("Tên ca");

                // Style cho Header (In đậm, nền xanh lá, căn giữa, viền bao quanh)
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);

                for (int i = 0; i < 3; i++) {
                    headerRow.getCell(i).setCellStyle(headerStyle);
                }

                // ==========================================
                // --- 4. FORMAT CỘT & DATA VALIDATION ---
                // ==========================================

                // Ép cột B (Ngày tháng) thành TEXT và căn giữa
                CellStyle textStyle = workbook.createCellStyle();
                DataFormat format = workbook.createDataFormat();
                textStyle.setDataFormat(format.getFormat("@"));
                textStyle.setAlignment(HorizontalAlignment.CENTER); // Căn giữa
                mainSheet.setDefaultColumnStyle(1, textStyle);

                // Căn giữa luôn cột A (Mã NV) và cột C (Ca) cho đồng bộ (tuỳ chọn)
                CellStyle centerStyle = workbook.createCellStyle();
                centerStyle.setAlignment(HorizontalAlignment.CENTER);
                mainSheet.setDefaultColumnStyle(0, centerStyle);
                mainSheet.setDefaultColumnStyle(2, centerStyle);

                DataValidationHelper validationHelper = mainSheet.getDataValidationHelper();

                // Dropdown cho "Mã nhân viên" (Bắt đầu từ dòng 2 - Index 1)
                CellRangeAddressList staffAddressList = new CellRangeAddressList(1, 1000, 0, 0);
                DataValidationConstraint staffConstraint = validationHelper.createFormulaListConstraint(
                        "HiddenData!$A$1:$A$" + staffCodes.size()
                );
                DataValidation staffValidation = validationHelper.createValidation(staffConstraint, staffAddressList);
                staffValidation.setShowErrorBox(true);
                mainSheet.addValidationData(staffValidation);

                // Dropdown cho "Tên ca" (Bắt đầu từ dòng 2 - Index 1)
                CellRangeAddressList shiftAddressList = new CellRangeAddressList(1, 1000, 2, 2);
                DataValidationConstraint shiftConstraint = validationHelper.createFormulaListConstraint(
                        "HiddenData!$B$1:$B$" + shiftNames.size()
                );
                DataValidation shiftValidation = validationHelper.createValidation(shiftConstraint, shiftAddressList);
                shiftValidation.setShowErrorBox(true);
                mainSheet.addValidationData(shiftValidation);

                // ==========================================
                // --- 5. KÉO RỘNG CÁC CỘT ---
                // ==========================================
                mainSheet.setColumnWidth(0, 20 * 256); // Cột Mã NV: rộng 20 character
                mainSheet.setColumnWidth(1, 25 * 256); // Cột Ngày: rộng 25 character
                mainSheet.setColumnWidth(2, 20 * 256); // Cột Tên ca: rộng 20 character

                // --- 6. XUẤT FILE RA BYTE ARRAY ---
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo file Excel Template: " + e.getMessage());
        }
    }
}