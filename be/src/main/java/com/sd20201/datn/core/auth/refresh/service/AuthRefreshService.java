package com.sd20201.datn.core.auth.refresh.service;

import com.sd20201.datn.core.auth.refresh.model.request.AuthRefreshRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface AuthRefreshService {

    ResponseObject<?> refresh(AuthRefreshRequest request);

}
