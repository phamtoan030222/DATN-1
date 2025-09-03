package com.sd20201.datn.core.admin.customer.controller;

import com.sd20201.datn.core.admin.customer.model.request.CustomerCreateUpdateRequest;
import com.sd20201.datn.core.admin.customer.model.request.CustomerRequest;
import com.sd20201.datn.core.admin.customer.service.CustomerService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_CUSTOMERS)
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    @GetMapping
    public ResponseEntity<?> getAllCustomers(@ModelAttribute CustomerRequest request){
        return Helper.createResponseEntity(customerService.getAllCustomers(request));
    }

    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody CustomerCreateUpdateRequest request){
        return Helper.createResponseEntity(customerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable String id,
            @RequestBody CustomerCreateUpdateRequest request
    ) {
        return Helper.createResponseEntity(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable String id) {
        return Helper.createResponseEntity(customerService.deleteCustomer(id));
    }
}
