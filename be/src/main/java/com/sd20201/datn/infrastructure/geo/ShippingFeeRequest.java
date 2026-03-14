package com.sd20201.datn.infrastructure.geo;

import lombok.Data;

@Data
public class ShippingFeeRequest {
    private String provinceName;
    private String wardName;
    private String address;
    private int weight;
    private long orderValue;
}
