package com.sd20201.datn.core.client.customer.controller;

import com.sd20201.datn.core.client.customer.service.ClientCustomerCartService;
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
@RequestMapping(MappingConstants.API_CUSTOMER_CART)
public class ClientCustomerCartController {

    private final ClientCustomerCartService cartService;

    @GetMapping("/{idCustomer}")
    public ResponseEntity<?> getCart(@PathVariable String idCustomer) {
        return Helper.createResponseEntity(cartService.getCartByCustomer(idCustomer));
    }

}
