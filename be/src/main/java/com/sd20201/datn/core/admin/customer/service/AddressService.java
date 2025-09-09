package com.sd20201.datn.core.admin.customer.service;

import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.core.admin.customer.model.request.AddressCreateUpdateRequest;
import com.sd20201.datn.core.admin.customer.model.request.AddressRequest;
import jakarta.validation.Valid;

public interface AddressService {
    ResponseObject<?> getAllAddresses(AddressRequest request);

    ResponseObject<?> getAddressesByCustomer(String customerId);

    ResponseObject<?> getDefaultAddress(String customerId);

    ResponseObject<?> addAddress(String customerId, @Valid AddressCreateUpdateRequest request);

    ResponseObject<?> updateAddress(String addressId, @Valid AddressCreateUpdateRequest request);

    ResponseObject<?> deleteAddress(String addressId);

    ResponseObject<?> setDefaultAddress(String customerId, String addressId);
}
