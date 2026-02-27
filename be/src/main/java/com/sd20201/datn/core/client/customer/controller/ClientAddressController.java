package com.sd20201.datn.core.client.customer.controller;

import com.sd20201.datn.core.admin.customer.service.AddressService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CUSTOMER API: Lấy địa chỉ mặc định của khách hàng.
 *
 * Path giống admin nhưng nằm dưới /api/v1/customer/... để FE client gọi đúng prefix.
 */
@RestController
@RequestMapping(MappingConstants.API_CUSTOMER_PREFIX_CUSTOMER_ADDRESSES)
@RequiredArgsConstructor
public class ClientAddressController {

    private final AddressService addressService;

    @GetMapping("/default")
    public ResponseEntity<?> getDefaultAddress(@PathVariable String customerId) {
        return Helper.createResponseEntity(addressService.getDefaultAddress(customerId));
    }
}
