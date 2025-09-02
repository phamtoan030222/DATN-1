package com.sd20201.datn.core.admin.customer.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequest extends PageableRequest {
private String customerName;

private Integer  customerStatus;

private Boolean customerGender;
}
