package com.sd20201.datn.core.client.customer.service;

import com.sd20201.datn.core.client.customer.model.request.ClientChangePasswordRequest;
import com.sd20201.datn.core.client.customer.model.request.ClientCustomerUpdateInformation;
import com.sd20201.datn.core.client.customer.model.request.ForgotPasswordRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import org.springframework.transaction.annotation.Transactional;

public interface ClientCustomerService {

    ResponseObject<?> updateInformation(ClientCustomerUpdateInformation request);

    // Đã thêm dấu @ bị thiếu
    @Transactional
    ResponseObject<?> changePassword(String username, ClientChangePasswordRequest request);

    @Transactional
    ResponseObject<?> forgotPassword(ForgotPasswordRequest request);
}
