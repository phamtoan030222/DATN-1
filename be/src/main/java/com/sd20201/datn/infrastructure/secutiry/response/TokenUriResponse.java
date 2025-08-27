package com.sd20201.datn.infrastructure.secutiry.response;

import com.sd20201.datn.utils.SecurityUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@AllArgsConstructor
@ToString
@Slf4j
public class TokenUriResponse {

    private String accessToken;

    private String refreshToken;

    public String getTokenAuthorizationSimple() {
        String tokenObject = "{" + "\"accessToken\":\"" + accessToken + "\",\"refreshToken\":\"" + refreshToken + "\"}";
        return SecurityUtil.encodeBase64(tokenObject);
    }

    public static String getState(
            String accessToken,
            String refreshToken
    ) {
        TokenUriResponse tokenUriResponse = new TokenUriResponse(accessToken, refreshToken);
        return tokenUriResponse.getTokenAuthorizationSimple();
    }

}
