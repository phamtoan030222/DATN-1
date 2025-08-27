package com.sd20201.datn.core.admin.products.color.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdColorRequest extends PageableRequest {

    private String colorName;

    private String colorStatus;
}
