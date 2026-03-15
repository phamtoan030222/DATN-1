package com.sd20201.datn.core.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentRequest {

    private String id;
    private String code;
    private BigDecimal totalAmountAfterDecrease; // tong tien

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String orderId;

    private String orderType = "billpayment";
    private String language = "vn";

    private String staffId;

    private BigDecimal tienHang;   // Tiền hàng gốc trước giảm
    private BigDecimal tienShip;   // Phí vận chuyển
    private String idPGG;          // ID voucher

    private String returnUrl;
}

