package com.sd20201.datn.core.client.products.product.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ClientProductRequest extends PageableRequest {

    private String idBrand;

    private String idBattery;

    private String idScreen;

    private String idOperatingSystem;

    private Integer minPrice;

    private Integer maxPrice;

}
