package com.sd20201.datn.core.admin.discount.voucher.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdVoucherRequest extends PageableRequest {

    public String voucherName;

    public Long startDate;

    public Long endDate;

    public EntityStatus status;

    public BigDecimal conditions;
}
