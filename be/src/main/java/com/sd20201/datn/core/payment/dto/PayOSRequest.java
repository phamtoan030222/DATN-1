package com.sd20201.datn.core.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PayOSRequest {
    private String invoiceId;
    private BigDecimal amount;
    private String description; // Tối đa 25 ký tự, không dấu
    private String returnUrl;   // FE override nếu cần
    private String cancelUrl;
}