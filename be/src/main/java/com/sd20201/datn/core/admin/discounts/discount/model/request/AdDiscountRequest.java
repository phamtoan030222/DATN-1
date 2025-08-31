package com.sd20201.datn.core.admin.discounts.discount.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdDiscountRequest extends PageableRequest {

    private String discountName;

    private Integer discountStatus;
}
