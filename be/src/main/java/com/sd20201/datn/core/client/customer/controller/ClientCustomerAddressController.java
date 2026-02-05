package com.sd20201.datn.core.client.customer.controller;

import com.sd20201.datn.core.client.customer.service.ClientCustomerAddressService;
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
@RequestMapping(MappingConstants.API_CUSTOMER_ADDRESS)
public class ClientCustomerAddressController {

    private final ClientCustomerAddressService customerService;

    @GetMapping("/customer/{idCustomer}")
    public ResponseEntity<?> getAddresses(@PathVariable String idCustomer) {
        return Helper.createResponseEntity(customerService.getAddressesByCustomer(idCustomer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddress(@PathVariable String id) {
        return Helper.createResponseEntity(customerService.getAddressById(id));
    }

}
