package com.sd20201.datn.core.client.customer.controller;

import com.sd20201.datn.core.client.customer.model.request.ClientChangePasswordRequest;
import com.sd20201.datn.core.client.customer.model.request.ClientCustomerUpdateInformation;
import com.sd20201.datn.core.client.customer.model.request.ForgotPasswordRequest;
import com.sd20201.datn.core.client.customer.service.ClientCustomerService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping(MappingConstants.API_CUSTOMER_PREFIX_CUSTOMERS)
@RequiredArgsConstructor
public class ClientCustomerController {

    private final ClientCustomerService customerService;

    @PostMapping
    public ResponseEntity<?> updateInformation(@RequestBody ClientCustomerUpdateInformation request) {
        return Helper.createResponseEntity(customerService.updateInformation(request));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ClientChangePasswordRequest request,
            Principal principal
    ) {
        // principal.getName() sẽ lấy ra username của khách hàng đang đăng nhập từ Token JWT
        String username = principal.getName();
        ResponseObject<?> response = customerService.changePassword(username, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ResponseObject<?> response = customerService.forgotPassword(request);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
