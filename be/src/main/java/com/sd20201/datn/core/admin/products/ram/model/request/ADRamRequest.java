package com.sd20201.datn.core.admin.products.ram.model.request;

import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
public class ADRamRequest extends PageableRequest {

    private String name;

}
