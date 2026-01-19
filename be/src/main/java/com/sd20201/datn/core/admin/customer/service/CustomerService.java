package com.sd20201.datn.core.admin.customer.service;

import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.core.admin.customer.model.request.CustomerCreateUpdateRequest;
import com.sd20201.datn.core.admin.customer.model.request.CustomerRequest;
import jakarta.validation.Valid;

public interface CustomerService {
    ResponseObject<?> getAllCustomers(CustomerRequest request);

    ResponseObject<?> createCustomer(@Valid CustomerCreateUpdateRequest request);

    ResponseObject<?> updateCustomer(String id, @Valid CustomerCreateUpdateRequest request);

    ResponseObject<?> deleteCustomer(String id);

    ResponseObject<?> getCustomerById(String id);

    ResponseObject<?> getCustomersWithStats(int page, int size, String keyword, String timeRange, String sortField, String sortDirection);
}
