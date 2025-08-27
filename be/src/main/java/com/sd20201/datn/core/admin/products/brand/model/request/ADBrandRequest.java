package com.sd20201.datn.core.admin.products.brand.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADBrandRequest extends PageableRequest {

    private String name;
}
