package com.sd20201.datn.core.admin.shift.repository;

import com.sd20201.datn.entity.ShiftHandover;
import com.sd20201.datn.repository.ShiftHandoverRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface AdShiftHandoverRepository extends ShiftHandoverRepository {


    @Query("SELECT s FROM ShiftHandover s WHERE s.account.id = :accountId AND s.status = com.sd20201.datn.infrastructure.constant.EntityStatus.ACTIVE")
    Optional<ShiftHandover> findOpenShiftByAccountId(@Param("accountId") String accountId);

    // 2. MỚI: Tìm ca đã đóng gần nhất (Sắp xếp theo giờ kết thúc giảm dần)
    // Lưu ý: Chúng ta dùng Pageable để giới hạn lấy 1 kết quả (LIMIT 1)
    @Query("SELECT s FROM ShiftHandover s WHERE s.status = com.sd20201.datn.infrastructure.constant.EntityStatus.INACTIVE ORDER BY s.endTime DESC")
    List<ShiftHandover> findLastClosedShift(Pageable pageable);

    @Query("SELECT s FROM ShiftHandover s " +
            "LEFT JOIN s.account a " +
            "LEFT JOIN a.staff st " +
            "WHERE (:keyword IS NULL OR :keyword = '' " +
            "OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(st.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:startDate IS NULL OR s.startTime >= :startDate) " +
            "AND (:endDate IS NULL OR s.startTime <= :endDate) " +
            "ORDER BY s.createdDate DESC")
    Page<ShiftHandover> searchHistory(
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}