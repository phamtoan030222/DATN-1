package com.sd20201.datn.core.client.invoice.service;

import com.sd20201.datn.core.client.invoice.model.request.ClientGetInvoicesRequest;
import com.sd20201.datn.core.client.invoice.model.request.ClientInvoiceCancelRequest;
import com.sd20201.datn.core.client.invoice.model.request.ClientPutInvoiceDetailRequest;
import com.sd20201.datn.core.client.invoice.model.request.ClientPutReceiverRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

import java.util.List;

public interface ClientInvoiceService {

    ResponseObject<?> getById(String code);

    ResponseObject<?> getHistoryInvoiceById(String id);

    ResponseObject<?> getInvoiceDetailsById(List<String> id);

    ResponseObject<?> getInvoiceByIdCustomer(ClientGetInvoicesRequest request);

    ResponseObject<?> cancelInvoice(ClientInvoiceCancelRequest request);

    ResponseObject<?> putReceiver(String id, ClientPutReceiverRequest request);

    ResponseObject<?> putInvoiceDetails(String id, ClientPutInvoiceDetailRequest request);
}
