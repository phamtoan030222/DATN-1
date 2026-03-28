package com.sd20201.datn.core.client.invoice.model.response;

import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.TypePayment;

import java.math.BigDecimal;

public interface ClientInvoiceDetailResponse {

    String getId();

    String getNameReceiver();

    String getEmail();

    String getAddressReceiver();

    String getPhoneReceiver();

    String getDescription();

    String getCode();

    Integer getInvoiceStatus();

    BigDecimal getTotalAmountAfterDecrease();

    BigDecimal getTotalAmount();

    Long getCreateDate();

    BigDecimal getShippingFee();

    String getTypePayment();

    String getTrangThaiThanhToan();

    String getIdCustomer();
}
