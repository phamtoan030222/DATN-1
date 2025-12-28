package com.sd20201.datn.core.admin.products.productdetail.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ADPDVariantRequest {

    private List<String> imei;

    private String idColor;

    private String idRAM;

    private String idHardDrive;

    private String idMaterial;

    private String idGPU;

    private String idCPU;

    private Double price;

    private String urlImage;

    private String publicId;

}
