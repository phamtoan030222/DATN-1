package com.sd20201.datn.core.admin.usercenter.service;

import com.sd20201.datn.core.admin.usercenter.model.request.DoiMatKhauRequest;

public interface UserCenterService {
    void doiMatKhau(String username, DoiMatKhauRequest request);
}
