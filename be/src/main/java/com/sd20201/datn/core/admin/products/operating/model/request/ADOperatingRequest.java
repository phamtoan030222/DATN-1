package com.sd20201.datn.core.admin.products.operating.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADOperatingRequest extends PageableRequest {

    private String key;
    private EntityStatus status;
}
