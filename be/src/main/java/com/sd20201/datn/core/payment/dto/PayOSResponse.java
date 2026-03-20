package com.sd20201.datn.core.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PayOSResponse {
    private String code;
    private String message;
    private String checkoutUrl;    // URL redirect đến trang PayOS
    private String qrCode;         // QR string nếu FE tự vẽ
    private String paymentLinkId;  // ID link thanh toán
    private String orderCode;      // Mã giao dịch PayOS
    private BigDecimal amount;
    private String invoiceId;
    private String invoiceCode;
}