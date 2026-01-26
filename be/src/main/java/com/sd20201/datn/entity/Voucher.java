package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "voucher")
public class Voucher extends PrimaryEntity implements Serializable {

    private Long startDate; // Ngày bắt đầu

    private Long endDate; // Ngày kết thúc

    @Enumerated(EnumType.ORDINAL)
    private TypeVoucher typeVoucher; // Enum: PERCENTAGE hoặc FIXED_AMOUNT

    private BigDecimal discountValue; // Giá trị giảm (%, hoặc số tiền)

    private BigDecimal maxValue; // Giá trị giảm tối đa

    private Integer quantity; // Số lượng ban đầu

    private Integer remainingQuantity; // Số lượng còn lại

    private BigDecimal conditions; //điều kiện nhận

    @Enumerated(EnumType.ORDINAL)
    private TargetType targetType; // Enum: INDIVIDUAL, ALL_CUSTOMERS

    private String note;


    @Transient // Không lưu vào database
    private BigDecimal giaTriGiamThucTe;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<VoucherDetail> voucherCustomers = new HashSet<>(); // Mối quan hệ many-to-many qua bảng trung gian

    /**
     * Kiem tra voucher co con hieu luc khong
     * @return true neu voucher con hieu luc
     */
    public boolean isValid() {
        Long currentTime = System.currentTimeMillis();
        return getStatus() == EntityStatus.ACTIVE &&
                remainingQuantity != null &&
                remainingQuantity > 0 &&
                startDate != null &&
                startDate <= currentTime &&
                endDate != null &&
                endDate >= currentTime;
    }

    /**
     * Kiem tra don hang co du dieu kien ap dung voucher khong
     * @pẩm totalAmount tong tien don hang
     * @return true neu du dieu kien
     */

    public boolean isEligible(BigDecimal totalAmount) {
        if (conditions == null){
            return true;
        }
        return totalAmount.compareTo(conditions) >= 0;
    }

    /**
     * Tinh toan gia tri giam thuc te cho don hang
     * @param totalAmount tong tien don hang
     * @return so tien duoc giam thuc te
     */

    public BigDecimal calculateActualDiscount(BigDecimal totalAmount) {
        //Kiem tra dieu kien ap dung
        if(!isEligible(totalAmount)) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = BigDecimal.ZERO;

        if (typeVoucher == TypeVoucher.PERCENTAGE) {
            //Tinh phan tram: totalAmount * discountValue /100
            discount = totalAmount
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            //Ap dung gioi han toi da neu co
            if (maxValue != null && discount.compareTo(maxValue) > 0) {
                discount = maxValue;
            }
        } else if (typeVoucher == TypeVoucher.FIXED_AMOUNT) {
            // Giam so tien co dinh, khong vuoit qua tong don hang
            discount = discountValue.min(totalAmount);
        }

        // Luu gia tri tinh toan vao transient field
        this.giaTriGiamThucTe = discount;
        return  discount;
    }

    /**
     * Kiem tra voucher co ap dung duoc cho khach hang khong
     * @param customerId ID khach hang(null neu chua dang nhap)
     * @return true neu khach hang duoc phep su dung
     */

    public boolean isApplicableForCustomer(String customerId) {
        // Voucher cong khai: ai cung dung duoc
        if (targetType == TargetType.ALL_CUSTOMERS) {
            return true;
        }

        // Voucher rieng tu: chi khach hang cu the dung doc
        if(targetType == TargetType.INDIVIDUAL) {
            if(customerId == null) {
                return false; //Chua duoc ap dung
            }

            //Kiem tra xem khach hang co trong danh sach nhan voucher khong
            return voucherCustomers.stream()
                    .anyMatch(vd -> vd.getCustomer() != null &&
                            customerId.equals(vd.getCustomer().getId()) &&
                            !Boolean.TRUE.equals(vd.getUsageStatus()));
        }
        return false;
    }

    /**
     * Giam so luong voucher khi co nguoi su dung
     * @return true neu giam thanh cong
     */
    public boolean decreaseQuantity() {
        if (remainingQuantity == null || remainingQuantity <= 0) {
            return false;
        }
        remainingQuantity--;
        return true;
    }

    /**
     * Chuyen doi timestamp thanh LocalDateTime
     * @return ngay bat dau dang LocalDateTime
     */

    public LocalDateTime getStartDateAsLocalDateTime() {
        if (startDate == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(startDate), ZoneId.systemDefault());
    }

    /**
     * Chuyen doi timestamp thanh LocalDateTime
     * @return ngay ket thuc dang LocalDateTime
     */
    public LocalDateTime getEndDateAsLocalDateTime() {
        if (endDate == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(endDate), ZoneId.systemDefault());
    }

    /**
     * Kiem tra voucher sap het han (Con < 24gio)
     * @return true neu sap het han
     */
    public boolean isExpiringSoon() {
        if (endDate == null) return false;
        long hoursLeft = (endDate - System.currentTimeMillis()) / (1000 *60 * 60);
        return hoursLeft < 24;
    }
}


