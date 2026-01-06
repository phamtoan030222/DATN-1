package com.sd20201.datn.core.admin.products.ram.model.request;

import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
public class ADRamRequest extends PageableRequest {

    private String key;
    private EntityStatus status;


}
