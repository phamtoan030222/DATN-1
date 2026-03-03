package com.sd20201.datn.core.client.invoice.model.response;

import java.math.BigDecimal;

public interface ClientInvoiceDetailResponse {

    String getId();

    String getCode();

    Integer getInvoiceStatus();

    BigDecimal getTotalAmountAfterDecrease();

    Integer getTotalAmount();
}
