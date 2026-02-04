package com.sd20201.datn.core.client.banhang.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClientThemSanPhamRequest {

    private String cartId;

    private String productDetailId;

    private Integer quantity;

}
