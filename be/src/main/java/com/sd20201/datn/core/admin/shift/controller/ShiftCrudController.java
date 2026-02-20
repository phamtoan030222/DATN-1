package com.sd20201.datn.core.admin.shift.controller;

import com.sd20201.datn.core.admin.shift.repository.AdWorkScheduleRepository;
import com.sd20201.datn.entity.Shift;
import com.sd20201.datn.repository.ShiftRepository;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(MappingConstants.API_SHIFTS)
@RequiredArgsConstructor
@CrossOrigin("*")
public class ShiftCrudController {

    private final ShiftRepository shiftRepository;
    private final AdWorkScheduleRepository workScheduleRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('QUAN_LY', 'NHAN_VIEN')")
    public ResponseEntity<List<Shift>> getAll() {
        List<Shift> shifts = shiftRepository.findAll();

        // 👇 Gắn cờ hasHistory cho từng ca làm việc để gửi về Frontend
        shifts.forEach(shift -> {
            boolean isUsed = workScheduleRepository.existsByShiftId(shift.getId());
            shift.setHasHistory(isUsed);
        });

        return ResponseEntity.ok(shifts);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Shift shift) {
        try {
            if (shift.getName() == null || shift.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tên ca không được để trống!");
            }
            if (shift.getStartTime() == null || shift.getEndTime() == null) {
                return ResponseEntity.badRequest().body("Vui lòng nhập đầy đủ thời gian!");
            }
            if (shift.getEndTime().compareTo(shift.getStartTime()) <= 0) {
                return ResponseEntity.badRequest().body("Giờ kết thúc phải lớn hơn giờ bắt đầu!");
            }

            String cleanName = shift.getName().trim();
            shift.setName(cleanName);

            if (shift.getId() == null) {
                // --- TRƯỜNG HỢP THÊM MỚI ---
                if (shiftRepository.existsByName(cleanName)) {
                    return ResponseEntity.badRequest().body("Tên ca '" + cleanName + "' đã tồn tại!");
                }
                if (shiftRepository.existsByStartTimeAndEndTime(shift.getStartTime(), shift.getEndTime())) {
                    return ResponseEntity.badRequest().body("Đã có ca làm việc khác có cùng khung giờ!");
                }
                shift.setStatus(EntityStatus.ACTIVE);
                if (shift.getCode() == null || shift.getCode().trim().isEmpty()) {
                    shift.setCode("CA" + System.currentTimeMillis() / 1000);
                }
            } else {
                // --- TRƯỜNG HỢP CẬP NHẬT ---
                if (shiftRepository.existsByNameAndIdNot(cleanName, shift.getId())) {
                    return ResponseEntity.badRequest().body("Tên ca '" + cleanName + "' đã được sử dụng!");
                }
                if (shiftRepository.existsByStartTimeAndEndTimeAndIdNot(shift.getStartTime(), shift.getEndTime(), shift.getId())) {
                    return ResponseEntity.badRequest().body("Đã có ca làm việc khác có cùng khung giờ!");
                }

                Shift oldShift = shiftRepository.findById(shift.getId()).orElse(null);

                // 👇 CHẶN SỬA GIỜ TẠI BACKEND: Bảo vệ lịch sử
                if (oldShift != null) {
                    boolean hasHistory = workScheduleRepository.existsByShiftId(shift.getId());
                    if (hasHistory) {
                        if (!oldShift.getStartTime().equals(shift.getStartTime()) ||
                                !oldShift.getEndTime().equals(shift.getEndTime())) {
                            return ResponseEntity.badRequest().body(
                                    "Ca này đã có lịch sử làm việc. Để đảm bảo tính toàn vẹn dữ liệu, bạn không được sửa khung giờ!"
                            );
                        }
                    }
                }

                // Chặn tắt ca nếu có lịch tương lai
                if (oldShift != null && oldShift.getStatus() == EntityStatus.ACTIVE && shift.getStatus() == EntityStatus.INACTIVE) {
                    boolean isInUse = workScheduleRepository.existsByShiftIdAndFutureDate(shift.getId());
                    if (isInUse) {
                        return ResponseEntity.badRequest().body(
                                "Không thể ngưng hoạt động! Ca này đang có lịch xếp cho nhân viên trong tương lai. Vui lòng gỡ lịch của nhân viên trước."
                        );
                    }
                }

                if (shift.getStatus() == null) {
                    shift.setStatus(EntityStatus.ACTIVE);
                }
            }

            return ResponseEntity.ok(shiftRepository.save(shift));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}