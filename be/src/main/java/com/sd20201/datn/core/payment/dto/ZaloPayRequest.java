package com.sd20201.datn.core.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ZaloPayRequest {
    private String invoiceId;
    private BigDecimal amount;
    private String description;
}