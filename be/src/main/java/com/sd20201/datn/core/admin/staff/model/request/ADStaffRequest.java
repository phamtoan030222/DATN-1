package com.sd20201.datn.core.admin.staff.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADStaffRequest extends PageableRequest {

    private String name;
    private String role;
    private String key;
    private String status;
}
