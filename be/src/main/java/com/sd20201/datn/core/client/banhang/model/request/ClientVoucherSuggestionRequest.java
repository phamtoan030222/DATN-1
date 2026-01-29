package com.sd20201.datn.core.client.banhang.model.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ClientVoucherSuggestionRequest {

    private String invoiceId;
    private BigDecimal tongTien;
    private String customerId;

}
