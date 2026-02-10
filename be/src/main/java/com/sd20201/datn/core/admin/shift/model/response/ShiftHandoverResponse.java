package com.sd20201.datn.core.admin.shift.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sd20201.datn.entity.ShiftHandover;
import com.sd20201.datn.entity.Staff; // <--- QUAN TRỌNG: Phải import cái này
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private BigDecimal initialCash;
    private BigDecimal totalCashAmount;
    private BigDecimal realCashAmount;
    private BigDecimal diffAmount;
    private String note;
    private Integer status;
    private Integer totalBills;

    public ShiftHandoverResponse(ShiftHandover entity) {
        this.id = entity.getId();

        // 👇 ĐOẠN CODE SỬA LỖI ĐỎ CỦA BẠN 👇
        if (entity.getAccount() != null) {
            // Lấy Staff từ Account
            Staff staff = entity.getAccount().getStaff();

            if (staff != null) {
                // Entity Staff có hàm getName() vì đã thêm ở Bước 1
                this.staffName = staff.getName();
                this.staffCode = staff.getCode();
            } else {
                this.staffName = entity.getAccount().getUsername();
                this.staffCode = "ADMIN";
            }
        }
        // 👆 ---------------------------- 👆

        this.startTime = entity.getStartTime();
        this.endTime = entity.getEndTime();
        this.totalBills = entity.getTotalBills();


        // Xử lý null an toàn
        this.initialCash = entity.getInitialCash() != null ? entity.getInitialCash() : BigDecimal.ZERO;
        this.totalCashAmount = entity.getTotalCashAmount() != null ? entity.getTotalCashAmount() : BigDecimal.ZERO;
        this.realCashAmount = entity.getRealCashAmount() != null ? entity.getRealCashAmount() : BigDecimal.ZERO;

        this.note = entity.getNote();

        if (entity.getStatus() != null) {
            this.status = entity.getStatus().ordinal();
        }

        // Tính chênh lệch
        if (entity.getRealCashAmount() != null) {
            BigDecimal lyThuyet = this.initialCash.add(this.totalCashAmount);
            this.diffAmount = this.realCashAmount.subtract(lyThuyet);
        } else {
            this.diffAmount = BigDecimal.ZERO;
        }
    }
}