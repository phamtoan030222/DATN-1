package com.sd20201.datn.core.admin.shift.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sd20201.datn.entity.ShiftHandover;
import com.sd20201.datn.entity.Staff;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ShiftHandoverResponse {

    private String id;
    private String staffName;
    private String staffCode;
    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private BigDecimal initialCash;
    private BigDecimal totalCashAmount; // Tổng tiền mặt hệ thống (Đã bao gồm tiền đầu ca + tiền mặt bán được)
    private BigDecimal realCashAmount;  // Tiền mặt nhân viên đếm thực tế
    private BigDecimal diffAmount;      // Tiền chênh lệch

    // Trường chuyển khoản để FE hiển thị
    private BigDecimal totalTransferAmount;

    private String note;
    private Integer status;
    private Integer totalBills;

    private String standardStartTime; // Giờ bắt đầu chuẩn (VD: "08:00:00")
    private String standardEndTime;   // Giờ kết thúc chuẩn (VD: "12:00:00")
    private BigDecimal totalRevenue;

    public ShiftHandoverResponse(ShiftHandover entity) {
        this.id = entity.getId();
        this.name = entity.getName();

        if (entity.getAccount() != null) {
            Staff staff = entity.getAccount().getStaff();
            if (staff != null) {
                this.staffName = staff.getName();
                this.staffCode = staff.getCode();
            } else {
                this.staffName = entity.getAccount().getUsername();
                this.staffCode = "ADMIN";
            }
        }

        this.startTime = entity.getStartTime();
        this.endTime = entity.getEndTime();
        this.totalBills = entity.getTotalBills();

        // Xử lý null an toàn cho tiền mặt
        this.initialCash = entity.getInitialCash() != null ? entity.getInitialCash() : BigDecimal.ZERO;
        this.totalCashAmount = entity.getTotalCashAmount() != null ? entity.getTotalCashAmount() : BigDecimal.ZERO;
        this.realCashAmount = entity.getRealCashAmount() != null ? entity.getRealCashAmount() : BigDecimal.ZERO;

        // 👇 ĐÃ MỞ KHÓA: Map dữ liệu tiền chuyển khoản từ Entity sang Response 👇
        this.totalTransferAmount = entity.getTotalTransferAmount() != null ? entity.getTotalTransferAmount() : BigDecimal.ZERO;

        // Doanh thu = (Tổng tiền hệ thống đang có - Tiền mồi đầu ca) + Tiền chuyển khoản
        this.totalRevenue = (this.totalCashAmount.subtract(this.initialCash)).add(this.totalTransferAmount);
        this.note = entity.getNote();

        if (entity.getStatus() != null) {
            this.status = entity.getStatus().ordinal();
        }

        // Tính chênh lệch chuẩn xác
        if (entity.getRealCashAmount() != null && entity.getTotalCashAmount() != null) {
            // Chênh lệch = Thực tế đếm được - Hệ thống tính toán (đã bao gồm đầu ca)
            this.diffAmount = this.realCashAmount.subtract(this.totalCashAmount);
        } else {
            this.diffAmount = BigDecimal.ZERO;
        }
    }
}