package com.sd20201.datn.core.admin.products.productdetail.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ADPDProductCreateRequest {
    private ProductRequest product;

    private List<VariantRequest> variant;

    @Setter
    @Getter
    public static class ProductRequest {
        private String name;

        private String code;

        private String idScreen;

        private String idBattery;

        private String idBrand;

        private String idOperatingSystem;
    }

    @Setter
    @Getter
    public static class VariantRequest {
        private List<String> imei;

        private String idColor;

        private String idRAM;

        private String idHardDrive;

        private String idMaterial;

        private String idGPU;

        private String idCPU;

        private Double price;

    }

}
