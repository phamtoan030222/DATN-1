package com.sd20201.datn.core.admin.customer.controller;

import com.sd20201.datn.core.admin.customer.model.request.AddressCreateUpdateRequest;
import com.sd20201.datn.core.admin.customer.service.AddressService;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_CUSTOMER_ADDRESSES)
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

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
    public ResponseEntity<?> addAddress(
            @PathVariable String customerId,
            @Valid @RequestBody AddressCreateUpdateRequest request
    ) {
        return Helper.createResponseEntity(addressService.addAddress(customerId, request));
    }

    // Update 1 địa chỉ
    @PutMapping("/{addressId}")
    public ResponseEntity<?> updateAddress(
            @PathVariable String addressId,
            @Valid @RequestBody AddressCreateUpdateRequest request
    ) {
        return Helper.createResponseEntity(addressService.updateAddress(addressId, request));
    }

    // Xóa địa chỉ
    @DeleteMapping("/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable String addressId) {
        return Helper.createResponseEntity(addressService.deleteAddress(addressId));
    }

    // Set mặc định địa chỉ
    @PutMapping("/{addressId}/set-default")
    public ResponseEntity<?> setDefaultAddress(
            @PathVariable String customerId,
            @PathVariable String addressId
    ) {
        return Helper.createResponseEntity(addressService.setDefaultAddress(customerId, addressId));
    }
}
