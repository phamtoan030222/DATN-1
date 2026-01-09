package com.sd20201.datn.core.auth.service;

import com.sd20201.datn.core.auth.model.request.AuthRefreshRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface AuthRefreshService {

    ResponseObject<?> refresh(AuthRefreshRequest request);

}
