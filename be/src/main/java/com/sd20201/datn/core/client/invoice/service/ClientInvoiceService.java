package com.sd20201.datn.core.client.invoice.service;

import com.sd20201.datn.core.client.invoice.model.request.ClientInvoiceCancelRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

import java.util.List;

public interface ClientInvoiceService {

    ResponseObject<?> getById(String code);

    ResponseObject<?> getHistoryInvoiceById(String id);

    ResponseObject<?> getInvoiceDetailsById(List<String> id);

    ResponseObject<?> getInvoiceByIdCustomer();

    ResponseObject<?> cancelInvoice(ClientInvoiceCancelRequest request);
}
