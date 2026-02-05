package com.sd20201.datn.core.admin.shift.service.impl;

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

import java.time.LocalDate;
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
}