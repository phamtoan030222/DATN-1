package com.sd20201.datn.core.client.products.productdetail.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class ClientPDProductDetailRequest extends PageableRequest {

    private List<String> idProduct;

    private List<String> idCPU;

    private List<String> idGPU;

    private List<String> idColor;

    private List<String> idRAM;

    private List<String> idHardDrive;

    private List<String> idMaterial;

    private List<String> idBrand;

    private List<String> idScreen;

    private Integer minPrice;

    private Integer maxPrice;
    
}
