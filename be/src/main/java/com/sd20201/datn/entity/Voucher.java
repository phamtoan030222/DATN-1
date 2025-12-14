package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
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
    private TargetType targetType; // Enum: LIMITED_BY_CONDITION, INDIVIDUAL, ALL_CUSTOMERS
    
    private String note;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<VoucherDetail> voucherCustomers = new HashSet<>(); // Mối quan hệ many-to-many qua bảng trung gian

}
