package com.sd20201.datn.core.admin.products.product.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ADProductCreateUpdateRequest {

    private String id;

    private String name;

    private String code;

    private String idScreen;

    private String idBattery;

    private String idBrand;

    private String idOperatingSystem;

}
