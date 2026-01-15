package com.sd20201.datn.core.admin.invoices.invoice.model.response;


import com.sd20201.datn.infrastructure.constant.TypeInvoice;

import java.math.BigDecimal;

public interface AdInvoiceResponse {
    String getId();

    String getCode();

    String getCustomerId();

    String getCustomerName();

    String getCustomerEmail();

    String getCustomerPhone();

    String getVoucherName();

    String getStaffName();

    String getStaffCode();

    BigDecimal getTotalAmount();

    BigDecimal getTotalAmountAfterDecrease();

    TypeInvoice getTypeInvoice();

    BigDecimal getShippingFee();

    String getNameReceiver();

    String getAddressReceiver();

    String getPhoneReceiver();

    String getPaymentDate();

    String getStatus();

    String getNote();
}
