package com.sd20201.datn.core.client.discounts.discountDetail.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientDiscountDetailRequest extends PageableRequest {
    private String productName;

    private String discountName;

    private Integer percentageVoucher;

}
