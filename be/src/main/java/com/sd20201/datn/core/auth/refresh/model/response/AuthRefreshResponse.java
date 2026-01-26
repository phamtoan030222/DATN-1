package com.sd20201.datn.core.auth.refresh.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AuthRefreshResponse {

    private String accessToken;

    private String refreshToken;

    public static AuthRefreshResponse of(String accessToken, String refreshToken) {
        return new AuthRefreshResponse(
                accessToken, refreshToken
        );
    }
}
