package com.sd20201.datn.infrastructure.geo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingFeeResponse {
    private int fee;
    private boolean success;
    private String message;
}
