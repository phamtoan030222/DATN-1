package com.sd20201.datn.core.client.customer.controller;

import com.sd20201.datn.core.client.customer.model.request.ClientCustomerUpdateInformation;
import com.sd20201.datn.core.client.customer.service.ClientCustomerService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MappingConstants.API_CUSTOMER_PREFIX_CUSTOMERS)
@RequiredArgsConstructor
public class ClientCustomerController {

    private final ClientCustomerService customerService;

    @PostMapping
    public ResponseEntity<?> updateInformation(@RequestBody ClientCustomerUpdateInformation request) {
        return Helper.createResponseEntity(customerService.updateInformation(request));
    }

}
