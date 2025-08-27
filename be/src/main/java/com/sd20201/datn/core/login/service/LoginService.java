package com.sd20201.datn.core.login.service;

import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.core.login.model.request.LoginRequest;
import com.sd20201.datn.core.login.model.response.LoginResponse;
import com.sd20201.datn.infrastructure.secutiry.response.TokenUriResponse;
import com.sd20201.datn.infrastructure.secutiry.service.RefreshTokenService;
import com.sd20201.datn.infrastructure.secutiry.service.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;

    private final TokenProvider tokenProvider;

    private final RefreshTokenService refreshTokenService;

    public ResponseObject<?> verify(LoginRequest loginRequest) {

        try {
            Authentication authentication =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            String token = tokenProvider.createTokenLogin(authentication);
            String refreshToken = refreshTokenService.createRefreshToken(authentication).getRefreshToken();

            return new ResponseObject<>(new LoginResponse(TokenUriResponse.getState(token, refreshToken)), HttpStatus.OK, "OKE");
        } catch (Exception e) {
            return ResponseObject.errorForward( "Login failure",HttpStatus.NOT_FOUND);
        }
    }
}
