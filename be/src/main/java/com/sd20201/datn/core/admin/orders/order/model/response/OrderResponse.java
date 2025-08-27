package com.sd20201.datn.core.admin.orders.order.model.response;

import java.math.BigDecimal;

public interface OrderResponse {
    String getId();
    String getCode();
    String getName();
    String getCustomerName();
    String getCustomerPhone();
    String getTypeInvoice();
    BigDecimal getTotalAmountAfterDecrease();
    Long getCreatedDate();
    String getStatus();
}
