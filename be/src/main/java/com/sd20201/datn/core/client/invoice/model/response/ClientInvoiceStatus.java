package com.sd20201.datn.core.client.invoice.model.response;

import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;

public interface ClientInvoiceStatus {
    String getId();

    String getCode();

    EntityTrangThaiHoaDon getStatus();


}
