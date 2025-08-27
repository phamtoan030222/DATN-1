package com.sd20201.datn.core.admin.products.material.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADCreateMaterialRequest {

    private String code;

    // Chất liệu nắp trên của laptop
    private String topCaseMaterial;

    // Chất liệu đáy laptop
    private String bottomCaseMaterial;

    // Chất liệu bàn phím
    private String keyboardMaterial;

    // Mô tả thêm nếu cần
    private String description;
}
