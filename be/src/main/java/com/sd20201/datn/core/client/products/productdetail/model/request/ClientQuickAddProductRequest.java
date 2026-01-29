package com.sd20201.datn.core.client.products.productdetail.model.request;

import com.sd20201.datn.infrastructure.constant.ProductPropertiesType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientQuickAddProductRequest {

    private String nameProperty;

    private ProductPropertiesType type;

}
