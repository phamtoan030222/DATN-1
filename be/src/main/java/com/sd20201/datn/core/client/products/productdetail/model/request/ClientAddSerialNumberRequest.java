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
public class ClientAddSerialNumberRequest {

    private String idProductDetail;

    private List<String> serialNumbers;

}
