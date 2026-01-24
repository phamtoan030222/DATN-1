package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.EntityProperties;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "voucher_detail")
public class VoucherDetail extends PrimaryEntity implements Serializable {

    @ManyToOne
    @JoinColumn(name = "id_voucher", referencedColumnName = "id")
    @JsonIgnore
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "id_customer", referencedColumnName = "id")
    private Customer customer;

    private Long usedDate;

    private Boolean usageStatus;

    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String description;

    @Column(name = "id_invoice")
    private String invoiceId;

    private Long receivedDate; // Thoi diem nhan voucher

    private Boolean expiredNotified = false; // Da thong bao sap het han chua

    /**
     * Kiem tra voucher detail nay co the su dung khong
     * @return true neu voucher chua dung va con hieu luc
     */
    public boolean canBeUsed() {
        return !Boolean.TRUE.equals(usageStatus) &&
                voucher != null &&
                voucher.isValid();
    }

    /**
     * Danh dau da su dung voucher
     * @param invoiceId ID hoa don su dung voucher
     */
    public void markAsUsed(String invoiceId) {
        this.usageStatus = true;
        this.usedDate = System.currentTimeMillis();
        this.invoiceId = invoiceId;
    }

    /**
     * Kiem tra voucher da het han chua
     * @return true neu da het han
     */
    public boolean isExpired() {
        return voucher != null &&
                voucher.getEndDate() != null &&
                System.currentTimeMillis() > voucher.getEndDate();
    }

    /**
     * Kiem tra voucher sap het han (con < 48h)
     * @return true neu sap het han
     */
    public boolean isExpiringSoon() {
        if (voucher == null || voucher.getEndDate() == null) return false;
        long hoursLeft = (voucher.getEndDate() - System.currentTimeMillis()) / (1000 * 60 * 60);
        return hoursLeft < 48 && hoursLeft > 0;
    }

    /**
     * Lay thong tin tom tat voucher
     * @return chuoi mo ta ngan
     */
    public String getSummary() {
        if (voucher == null) return "Voucher khong ton tai";

        String type = voucher.getTypeVoucher() == TypeVoucher.PERCENTAGE ?
                voucher.getDiscountValue() + "%" :
                voucher.getDiscountValue() + "VND";

        return String.format("%s - Giam %s - HSD: %s",
                voucher.getCode(),
                type,
                voucher.getEndDateAsLocalDateTime());
    }
}
