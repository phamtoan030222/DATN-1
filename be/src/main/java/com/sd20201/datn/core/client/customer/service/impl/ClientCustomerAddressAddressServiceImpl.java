package com.sd20201.datn.core.client.customer.service.impl;

import com.sd20201.datn.core.client.customer.repository.ClientCustomerAddressRepository;
import com.sd20201.datn.core.client.customer.service.ClientCustomerAddressService;
import com.sd20201.datn.core.common.base.ResponseObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientCustomerAddressAddressServiceImpl implements ClientCustomerAddressService {

    private final ClientCustomerAddressRepository addressRepository;

    @Override
    public ResponseObject<?> getAddressesByCustomer(String idCustomer) {
        return ResponseObject.successForward(addressRepository.getAddressByCustomerId(idCustomer), "OKE");
    }

    @Override
    public ResponseObject<?> getAddressById(String id) {
        return addressRepository.findAddressById(id)
                .map(response -> ResponseObject.successForward(response, "OKE"))
                .orElse(ResponseObject.errorForward("Address not found", HttpStatus.NOT_FOUND));
    }
}
