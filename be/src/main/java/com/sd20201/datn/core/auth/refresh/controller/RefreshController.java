package com.sd20201.datn.core.auth.refresh.controller;

import com.sd20201.datn.core.auth.refresh.model.request.AuthRefreshRequest;
import com.sd20201.datn.core.auth.refresh.service.AuthRefreshService;
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
@RequestMapping(MappingConstants.API_AUTH_PREFIX)
public class RefreshController {

    private final AuthRefreshService authRefreshService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody AuthRefreshRequest request) {
        return Helper.createResponseEntity(authRefreshService.refresh(request));
    }

}
