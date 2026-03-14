package com.sd20201.datn.core.client.customer.service;

import com.sd20201.datn.core.client.customer.model.request.ClientCustomerUpdateInformation;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ClientCustomerService {

    ResponseObject<?> updateInformation(ClientCustomerUpdateInformation request);
}
