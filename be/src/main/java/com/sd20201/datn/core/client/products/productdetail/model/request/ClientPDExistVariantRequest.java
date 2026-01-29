package com.sd20201.datn.core.client.products.productdetail.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClientPDExistVariantRequest {

    private String productId;

    private List<PropertiesVariant> listPropertiesVariant;

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertiesVariant {
        private String idGPU;

        private String idCPU;

        private String idRAM;

        private String idHardDrive;

        private String idMaterial;

        private String idColor;
    }

}
