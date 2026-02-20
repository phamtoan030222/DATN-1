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
}