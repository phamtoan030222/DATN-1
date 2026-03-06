package com.sd20201.datn.core.client.customer.controller;

import com.sd20201.datn.core.admin.customer.model.request.AddressCreateUpdateRequest;
import com.sd20201.datn.core.admin.customer.service.AddressService;
import com.sd20201.datn.core.client.customer.service.ClientCustomerAddressService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CUSTOMER API: Lấy địa chỉ mặc định của khách hàng.
 * <p>
 * Path giống admin nhưng nằm dưới /api/v1/customer/... để FE client gọi đúng prefix.
 */
@RestController
@RequestMapping(MappingConstants.API_CUSTOMER_PREFIX_CUSTOMER_ADDRESSES)
@RequiredArgsConstructor
public class ClientAddressController {

    private final ClientCustomerAddressService addressService;

    // Lấy tất cả địa chỉ của 1 customer
    @GetMapping
    public ResponseEntity<?> getAddressesByCustomer(@PathVariable String customerId) {
        return Helper.createResponseEntity(addressService.getAddressesByCustomer(customerId));
    }

    // Lấy địa chỉ mặc định
    @GetMapping("/default")
    public ResponseEntity<?> getDefaultAddress(@PathVariable String customerId) {
        return Helper.createResponseEntity(addressService.getDefaultAddress(customerId));
    }

    // Tạo mới địa chỉ cho customer
    @PostMapping
    public ResponseEntity<?> addAddress(@PathVariable String customerId, @Valid @RequestBody AddressCreateUpdateRequest request) {
        return Helper.createResponseEntity(addressService.addAddress(customerId, request));
    }

    // Update 1 địa chỉ
    @PutMapping("/{addressId}")
    public ResponseEntity<?> updateAddress(@PathVariable String addressId, @Valid @RequestBody AddressCreateUpdateRequest request) {
        return Helper.createResponseEntity(addressService.updateAddress(addressId, request));
    }

    // Xóa địa chỉ
    @DeleteMapping("/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable String addressId) {
        return Helper.createResponseEntity(addressService.deleteAddress(addressId));
    }

    // Set mặc định địa chỉ
    @PatchMapping("/{addressId}/set-default")
    public ResponseEntity<?> setDefaultAddress(@PathVariable String customerId, @PathVariable String addressId) {
        return Helper.createResponseEntity(addressService.setDefaultAddress(customerId, addressId));
    }
}
