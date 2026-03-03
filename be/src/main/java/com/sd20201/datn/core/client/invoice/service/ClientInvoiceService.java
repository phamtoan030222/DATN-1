package com.sd20201.datn.core.client.invoice.service;

import com.sd20201.datn.core.common.base.ResponseObject;

public interface ClientInvoiceService {

    ResponseObject<?> getById(String code);
}
