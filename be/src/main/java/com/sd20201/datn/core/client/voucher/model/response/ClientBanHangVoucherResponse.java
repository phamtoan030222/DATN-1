package com.sd20201.datn.core.client.voucher.model.response;

import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;

import java.math.BigDecimal;

public interface ClientBanHangVoucherResponse {

    String getId();

    String getName();

    Long getStartDate();

    Long getEndDate();

    BigDecimal getMaxValue();

    BigDecimal getDiscountValue();

    BigDecimal getConditions();

    TypeVoucher getTypeVoucher();

    TargetType getTargetType();
}
