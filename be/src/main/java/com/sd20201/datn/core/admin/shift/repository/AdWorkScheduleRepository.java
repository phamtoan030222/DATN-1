package com.sd20201.datn.core.admin.shift.repository;

import com.sd20201.datn.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AdWorkScheduleRepository extends JpaRepository<WorkSchedule, String> {

    // 1. Lấy lịch để hiển thị lên Calendar
    @Query("SELECT w FROM WorkSchedule w WHERE w.workDate BETWEEN :fromDate AND :toDate")
    List<WorkSchedule> findByWorkDateBetween(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    WorkSchedule findByShiftIdAndWorkDate(String shiftId, LocalDate workDate);

    // 2. Dùng cho nút TẮT/BẬT (Toggle): Chỉ chặn nếu có lịch ở TƯƠNG LAI
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN TRUE ELSE FALSE END " +
            "FROM WorkSchedule w WHERE w.shift.id = :shiftId AND w.workDate >= CURRENT_DATE")
    boolean existsByShiftIdAndFutureDate(@Param("shiftId") String shiftId);

    // 👇👇👇 3. Dùng cho nút XÓA (Delete): Chặn nếu đã từng dùng (bất kể quá khứ hay tương lai)
    // Spring Data JPA tự động hiểu: "Kiểm tra xem có bản ghi nào chứa shiftId này không?"
    boolean existsByShiftId(String shiftId);
}