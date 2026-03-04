package com.sd20201.datn.repository;

import com.sd20201.datn.entity.LichSuThanhToan;
import com.sd20201.datn.infrastructure.constant.TrangThaiThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface LichSuThanhToanRepository extends JpaRepository<LichSuThanhToan, Long> {
    @Query("SELECT SUM(l.soTien) FROM LichSuThanhToan l " +
            "WHERE l.hoaDon.shiftHandover.id = :shiftId " +
            "AND l.loaiGiaoDich = :loaiGiaoDich " +
            "AND l.trangThaiThanhToan = 1") // Số 1 tương ứng với Đã thanh toán trong DB của bạn
    BigDecimal sumAmountByShiftIdAndMethod(
            @Param("shiftId") String shiftId,
            @Param("loaiGiaoDich") String loaiGiaoDich
    );
}
