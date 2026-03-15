package com.sd20201.datn.core.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MomoPaymentResponse {
    private String partnerCode;
    private String accessKey;
    private String requestId;
    private String amount;
    private String orderId;
    private String responseTime;
    private String message;
    private String localMessage;
    private String requestType;
    private String payUrl;      // ← URL redirect sang MoMo
    private String deeplink;
    private String qrCodeUrl;
    private int resultCode;     // 0 = thành công
}