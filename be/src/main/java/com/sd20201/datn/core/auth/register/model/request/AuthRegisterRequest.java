package com.sd20201.datn.core.auth.register.model.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthRegisterRequest {

    private String username;

    private String password;

    private String fullName;

    private Long birthday;

    private String phoneNumber;

    private String email;

}
