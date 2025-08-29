package com.sd20201.datn.core.admin.discount.voucher.motel.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdVoucherRequest extends PageableRequest {



    public String voucherName;

    public Long startDate;

    public Long endDate;

    public String voucherStatus;

}
