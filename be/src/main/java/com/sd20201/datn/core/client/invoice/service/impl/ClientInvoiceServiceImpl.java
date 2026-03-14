package com.sd20201.datn.core.client.invoice.service.impl;

import com.sd20201.datn.core.client.invoice.repository.ClientInvoiceRepository;
import com.sd20201.datn.core.client.invoice.service.ClientInvoiceService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.utils.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientInvoiceServiceImpl implements ClientInvoiceService {

    private final ClientInvoiceRepository invoiceRepository;

    private final UserContextHelper userContextHelper;

    @Override
    public ResponseObject<?> getById(String code) {
        Optional<String> customerIdOptional = userContextHelper.getCurrentUserId();

        return invoiceRepository.getInvoiceByCode(code, customerIdOptional.orElse(null))
                .map(invoice -> ResponseObject.successForward(invoice, "Fetch invoice success"))
                .orElse(ResponseObject.errorForward("Fetch invoice failure", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> getHistoryInvoiceById(String idHoaDon) {
        return ResponseObject.successForward(
                invoiceRepository.getInvoiceLichSuTrangThaiHoaDonByIdHoaDon(idHoaDon, userContextHelper.getCurrentUserId().orElse(null)),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getInvoiceDetailsById(List<String> ids) {
        return ResponseObject.successForward(
                invoiceRepository.getInvoiceDetailsByInvoiceId(ids),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getInvoiceByIdCustomer() {
        return ResponseObject.successForward(
                invoiceRepository.getInvoicesByIdCustomer(userContextHelper.getCurrentUserId().orElse(null)),
                "OKE"
        );
    }
}
