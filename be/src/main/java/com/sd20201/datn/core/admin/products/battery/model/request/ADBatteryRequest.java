package com.sd20201.datn.core.admin.products.battery.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADBatteryRequest extends PageableRequest {
    private String key;
}
