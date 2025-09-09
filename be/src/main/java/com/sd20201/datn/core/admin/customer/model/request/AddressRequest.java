package com.sd20201.datn.core.admin.customer.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest extends PageableRequest {
    private String customerId;
    private Boolean isDefault;
    private String provinceCity;
}
