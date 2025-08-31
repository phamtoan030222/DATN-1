package com.sd20201.datn.core.admin.discounts.discount.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdDscountFilterRequest extends PageableRequest {
    private String discountName;

    private String discountCode;

    private Integer discountStatus;

    private Integer percentage;

    private String description;

    private Long startDate;

    private Long endDate;

}
