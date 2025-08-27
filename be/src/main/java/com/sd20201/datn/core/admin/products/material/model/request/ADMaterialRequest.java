package com.sd20201.datn.core.admin.products.material.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADMaterialRequest extends PageableRequest {

    private String key;
}
