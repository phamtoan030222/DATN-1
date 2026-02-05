package com.sd20201.datn.core.admin.shift.controller;

import com.sd20201.datn.core.admin.shift.repository.AdWorkScheduleRepository; // <--- 1. NHỚ IMPORT CÁI NÀY
import com.sd20201.datn.entity.Shift;
import com.sd20201.datn.repository.ShiftRepository;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(MappingConstants.API_SHIFTS)
@RequiredArgsConstructor
@CrossOrigin("*")
public class ShiftCrudController {

    private final ShiftRepository shiftRepository;

    // 👇 2. KHAI BÁO REPOSITORY LỊCH LÀM VIỆC ĐỂ CHECK
    private final AdWorkScheduleRepository workScheduleRepository;

    @GetMapping
    public ResponseEntity<List<Shift>> getAll() {
        return ResponseEntity.ok(shiftRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Shift shift) {
        try {
            // 1. Validate cơ bản (Giữ nguyên)
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

            // 2. LOGIC CHECK TRÙNG (SỬA LẠI ĐOẠN NÀY) 👇👇👇

            if (shift.getId() == null) {
                // --- TRƯỜNG HỢP THÊM MỚI ---

                // Check tên
                if (shiftRepository.existsByName(cleanName)) {
                    return ResponseEntity.badRequest().body("Tên ca '" + cleanName + "' đã tồn tại!");
                }

                // Check trùng giờ y hệt
                if (shiftRepository.existsByStartTimeAndEndTime(shift.getStartTime(), shift.getEndTime())) {
                    return ResponseEntity.badRequest().body("Đã có ca làm việc khác có cùng khung giờ (" + shift.getStartTime() + " - " + shift.getEndTime() + ")!");
                }

                // Set default data
                shift.setStatus(EntityStatus.ACTIVE);
                if (shift.getCode() == null || shift.getCode().trim().isEmpty()) {
                    shift.setCode("CA" + System.currentTimeMillis() / 1000);
                }

            } else {
                // --- TRƯỜNG HỢP CẬP NHẬT ---

                // Check tên (trừ chính nó)
                if (shiftRepository.existsByNameAndIdNot(cleanName, shift.getId())) {
                    return ResponseEntity.badRequest().body("Tên ca '" + cleanName + "' đã được sử dụng!");
                }

                // Check trùng giờ (trừ chính nó)
                if (shiftRepository.existsByStartTimeAndEndTimeAndIdNot(shift.getStartTime(), shift.getEndTime(), shift.getId())) {
                    return ResponseEntity.badRequest().body("Đã có ca làm việc khác có cùng khung giờ (" + shift.getStartTime() + " - " + shift.getEndTime() + ")!");
                }

                // ... (Đoạn code chặn tắt ca giữ nguyên như cũ) ...
                Shift oldShift = shiftRepository.findById(shift.getId()).orElse(null);
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            // Check toàn bộ lịch sử (Quá khứ + Tương lai)
            boolean hasHistory = workScheduleRepository.existsByShiftId(id);

            if (hasHistory) {
                // Nếu đã từng dùng -> Bắt buộc dùng tính năng Tắt (Inactive) chứ không được Xóa
                return ResponseEntity.badRequest().body(
                        "Không thể xóa ca này vì đã phát sinh dữ liệu lịch sử chấm công. Vui lòng chọn 'Ngưng hoạt động' thay vì xóa."
                );
            }

            shiftRepository.deleteById(id);
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống!");
        }
    }
}