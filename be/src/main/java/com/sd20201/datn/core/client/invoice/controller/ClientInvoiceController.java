package com.sd20201.datn.core.client.invoice.controller;

import com.sd20201.datn.core.client.invoice.service.ClientInvoiceService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_INVOICE_ORDER_ONLINE)
public class ClientInvoiceController {

    private final ClientInvoiceService invoiceService;

    @GetMapping("/{q}")
    public ResponseEntity<?> get(@PathVariable("q") String code) {
        return Helper.createResponseEntity(invoiceService.getById(code));
    }

    @GetMapping("/user")
    public ResponseEntity<?> getInvoiceByCustomer() {
        return Helper.createResponseEntity(invoiceService.getInvoiceByIdCustomer());
    }

    @GetMapping("/invoices-detail")
    public ResponseEntity<?> getInvoiceDetails(@RequestParam List<String> ids) {
        return Helper.createResponseEntity(invoiceService.getInvoiceDetailsById(ids));
    }
    
    @GetMapping("/{id}/histories")
    public ResponseEntity<?> getHistory(@PathVariable("id") String id) {
        return Helper.createResponseEntity(invoiceService.getHistoryInvoiceById(id));
    }
}
