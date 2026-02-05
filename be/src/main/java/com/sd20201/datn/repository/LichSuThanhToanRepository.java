package com.sd20201.datn.repository;

import com.sd20201.datn.entity.LichSuThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface LichSuThanhToanRepository extends JpaRepository<LichSuThanhToan, Long> {
    /**
     * Tính tổng tiền mặt thu được của nhân viên trong khoảng thời gian.
     * @param accountId: ID tài khoản nhân viên
     * @param type: Loại giao dịch (VD: "TIEN_MAT" hoặc "CASH" - tùy DB bạn lưu là gì)
     * @param start: Thời gian bắt đầu (LocalDateTime)
     * @param end: Thời gian kết thúc (LocalDateTime)
     */
    @Query("SELECT COALESCE(SUM(lstt.soTien), 0) FROM LichSuThanhToan lstt " +
            "WHERE lstt.nhanVien.account.id = :accountId " +
            "AND lstt.loaiGiaoDich = :type " +
            "AND lstt.thoiGian BETWEEN :start AND :end")
    BigDecimal sumTotalCash(String accountId, String type, LocalDateTime start, LocalDateTime end);

}
