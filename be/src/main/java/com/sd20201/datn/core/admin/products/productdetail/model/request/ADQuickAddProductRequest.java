package com.sd20201.datn.core.admin.products.productdetail.model.request;

import com.sd20201.datn.infrastructure.constant.ProductPropertiesType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADQuickAddProductRequest {

    private String nameProperty;

    private ProductPropertiesType type;

}
