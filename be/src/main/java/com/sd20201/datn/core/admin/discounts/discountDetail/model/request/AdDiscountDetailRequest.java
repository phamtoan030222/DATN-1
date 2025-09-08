package com.sd20201.datn.core.admin.discounts.discountDetail.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdDiscountDetailRequest extends PageableRequest {
    private String productName;

    private String discountName;

    private Integer percentageVoucher;

}
