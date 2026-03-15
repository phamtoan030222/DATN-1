package com.sd20201.datn.core.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MomoPaymentRequest {
    private String partnerCode;
    private String accessKey;
    private String requestId;
    private String amount;
    private String orderId;
    private String orderInfo;
    private String returnUrl;
    private String notifyUrl;
    private String requestType;
    private String extraData;
    private String signature;
}