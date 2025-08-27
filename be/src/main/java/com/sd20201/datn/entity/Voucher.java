package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "voucher")
public class Voucher extends PrimaryEntity implements Serializable {

    @Enumerated(EnumType.ORDINAL)
    private TypeVoucher typeVoucher;

    private Integer decreaseUnit;

    private BigDecimal increaseUnit;

    private Long startTime;

    private Long endTime;

    private BigDecimal conditionOfUse;

}
