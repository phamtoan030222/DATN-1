package com.sd20201.datn.core.auth.refresh.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthRefreshRequest {

    private String refreshToken;

}
