package com.sd20201.datn.core.auth.register.controller;

import com.sd20201.datn.core.auth.register.model.request.AuthRegisterRequest;
import com.sd20201.datn.core.auth.register.service.RegisterService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(MappingConstants.API_AUTH_REGISTER)
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping("/customer")
    ResponseEntity<?> registerCustomer(@RequestBody AuthRegisterRequest request) {
        return Helper.createResponseEntity(registerService.register(request));
    }

}
