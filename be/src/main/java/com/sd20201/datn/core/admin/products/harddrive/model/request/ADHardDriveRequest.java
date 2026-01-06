package com.sd20201.datn.core.admin.products.harddrive.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADHardDriveRequest extends PageableRequest {

    private String key;
    private EntityStatus status;

}
