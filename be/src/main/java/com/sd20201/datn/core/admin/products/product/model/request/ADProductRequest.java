package com.sd20201.datn.core.admin.products.product.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ADProductRequest extends PageableRequest {

    private String idBrand;

    private String idBattery;

    private String idScreen;

    private String idOperatingSystem;

}
