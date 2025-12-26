package com.sd20201.datn.core.admin.customer.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequest extends PageableRequest {
    private String keyword;

    private Integer customerStatus;

    private Boolean customerGender;

    public String getKeyword() {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim();
    }
}
