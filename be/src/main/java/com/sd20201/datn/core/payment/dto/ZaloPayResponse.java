package com.sd20201.datn.core.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor  // ← THÊM
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZaloPayResponse {
    private int returnCode;      // 1 = thành công
    private String returnMessage;
    private int subReturnCode;
    private String subReturnMessage;
    private String orderUrl;     // ← URL redirect sang ZaloPay
    private String zptransToken;
    private String orderToken;
    private String qrCode;
}