package com.sd20201.datn.core.client.invoice.service.impl;

import com.sd20201.datn.core.client.invoice.repository.ClientInvoiceRepository;
import com.sd20201.datn.core.client.invoice.service.ClientInvoiceService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.utils.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientInvoiceServiceImpl implements ClientInvoiceService {

    private final ClientInvoiceRepository invoiceRepository;

    private final UserContextHelper userContextHelper;

    @Override
    public ResponseObject<?> getById(String code) {
        Optional<String> customerIdOptional = userContextHelper.getCurrentUserId();
        if (customerIdOptional.isEmpty()) return ResponseObject.errorForward("User not found", HttpStatus.NOT_FOUND);

        return invoiceRepository.getInvoiceByCode(code, customerIdOptional.get())
                .map(invoice -> ResponseObject.successForward(invoice, "Fetch invoice success"))
                .orElse(ResponseObject.errorForward("Fetch invoice failure", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> getHistoryInvoiceById(String idHoaDon) {
        return ResponseObject.successForward(
                invoiceRepository.getInvoiceLichSuTrangThaiHoaDonByIdHoaDon(idHoaDon),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getInvoiceDetailsById(String id) {
        return ResponseObject.successForward(
                invoiceRepository.getInvoiceDetailsByInvoiceId(id),
                "OKE"
        );
    }
}
