package com.sd20201.datn.core.client.customer.service;

import com.sd20201.datn.core.common.base.ResponseObject;

public interface ClientCustomerAddressService {

    ResponseObject<?> getAddressesByCustomer(String idCustomer);

    ResponseObject<?> getAddressById(String id);
}
