package com.sd20201.datn.core.admin.voucher.voucher.model.response;

import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;

import java.math.BigDecimal;

public interface AdVoucherResponse {

    String getId();

    String getCode();

    String getName();

    Long getStartDate();

    Long getEndDate();

    TypeVoucher getTypeVoucher();

    BigDecimal getDiscountValue();

    BigDecimal getMaxValue();

    Integer getQuantity();

    Integer getRemainingQuantity();

    BigDecimal getConditions();

    TargetType getTargetType();

    String getNote();

    Integer getStatus();

    Long getCreatedDate();
}

