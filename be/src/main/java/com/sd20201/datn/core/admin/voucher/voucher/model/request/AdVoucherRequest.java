package com.sd20201.datn.core.admin.voucher.voucher.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdVoucherRequest extends PageableRequest {

    TypeVoucher typeVoucher;

    TargetType targetType;

    public Long startDate;

    public Long endDate;

    public EntityStatus status;

    public BigDecimal conditions;

    // THÊM biến này để lọc Sắp/Đang/Kết thúc
    private String period;

    private Long now = System.currentTimeMillis();
}
