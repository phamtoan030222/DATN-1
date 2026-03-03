package com.sd20201.datn.core.client.invoice.controller;

import com.sd20201.datn.core.client.invoice.service.ClientInvoiceService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_CUSTOMER_HOA_DON)
public class ClientInvoiceController {

    private final ClientInvoiceService invoiceService;

    @GetMapping("/{code}")
    public ResponseEntity<?> get(@PathVariable("code") String code) {
        return Helper.createResponseEntity(invoiceService.getById(code));
    }
}
