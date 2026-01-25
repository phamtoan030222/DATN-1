package com.sd20201.datn.core.client.discounts.discount.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientDiscountRequest extends PageableRequest {

    private String discountName;

    private Integer discountStatus;
}
