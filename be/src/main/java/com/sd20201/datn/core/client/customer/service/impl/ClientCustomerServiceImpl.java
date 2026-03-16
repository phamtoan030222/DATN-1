package com.sd20201.datn.core.client.customer.service.impl;

import com.sd20201.datn.core.client.customer.model.request.ClientCustomerUpdateInformation;
import com.sd20201.datn.core.client.customer.service.ClientCustomerService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.repository.CustomerRepository;
import com.sd20201.datn.utils.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientCustomerServiceImpl implements ClientCustomerService {

    private final UserContextHelper userContextHelper;

    private final CustomerRepository customerRepository;

    @Override
    public ResponseObject<?> updateInformation(final ClientCustomerUpdateInformation request) {
        return userContextHelper.getCurrentUserId()
                .flatMap(customerRepository::findById)
                .map(customer -> {
                    customer.setPhone(request.getPhoneNumber());
                    customer.setEmail(request.getEmail());
                    customer.setName(request.getFullName());
                    customerRepository.save(customer);

                    return ResponseObject.successForward(customer.getId(), "OKE");
                })
                .orElse(ResponseObject.errorForward("Customer not found", HttpStatus.NOT_FOUND));
    }
}
