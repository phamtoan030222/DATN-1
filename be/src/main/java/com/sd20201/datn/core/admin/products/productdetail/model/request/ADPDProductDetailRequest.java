package com.sd20201.datn.core.admin.products.productdetail.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class ADPDProductDetailRequest extends PageableRequest {

    private String idProduct;

    private String idCPU;

    private String idGPU;

    private String idColor;

    private String idRAM;

    private String idHardDrive;

    private String idMaterial;
    
}
