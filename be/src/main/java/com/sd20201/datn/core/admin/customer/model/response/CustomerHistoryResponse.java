package com.sd20201.datn.core.admin.customer.model.response;

import java.math.BigDecimal;

public interface CustomerHistoryResponse {
    String getId();

    String getCustomerName();

    String getCustomerPhone();

    String getCustomerEmail();

    String getCustomerAvatar();

    String getCustomerCode();

    BigDecimal getLastOrderValue();

    Long getLastOrderDate();
}
