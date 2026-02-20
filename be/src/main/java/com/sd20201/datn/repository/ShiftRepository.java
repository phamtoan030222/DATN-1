package com.sd20201.datn.repository;

import com.sd20201.datn.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, String> {
    // Repository này để lấy danh sách Ca mẫu (Sáng/Chiều)
    // 1. Kiểm tra xem tên có tồn tại chưa (Dùng cho Thêm mới)
    boolean existsByName(String name);

    // 2. Kiểm tra xem tên có trùng với ca KHÁC không (Dùng cho Cập nhật)
    // Nghĩa là: Tìm xem có thằng nào tên giống thế này mà ID KHÔNG PHẢI là thằng đang sửa không
    boolean existsByNameAndIdNot(String name, String id);

    boolean existsByStartTimeAndEndTime(String startTime, String endTime);

    // 2. Check khi Cập nhật: Có ai trùng Start VÀ End (trừ chính mình ra) không?
    boolean existsByStartTimeAndEndTimeAndIdNot(String startTime, String endTime, String id);

    // Trong interface ShiftRepository
    Shift findByName(String name);
}
