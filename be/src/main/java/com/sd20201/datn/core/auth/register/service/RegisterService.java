package com.sd20201.datn.core.auth.register.service;

import com.sd20201.datn.core.auth.register.model.request.AuthRegisterRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface RegisterService {

    ResponseObject<?> register(AuthRegisterRequest request);

}
