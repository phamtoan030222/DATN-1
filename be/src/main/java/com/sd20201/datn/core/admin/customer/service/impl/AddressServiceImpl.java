package com.sd20201.datn.core.admin.customer.service.impl;

import com.sd20201.datn.core.admin.customer.model.request.AddressCreateUpdateRequest;
import com.sd20201.datn.core.admin.customer.model.request.AddressRequest;
import com.sd20201.datn.core.admin.customer.model.response.AddressResponse;
import com.sd20201.datn.core.admin.customer.repository.AdAddressRepository;
import com.sd20201.datn.core.admin.customer.service.AddressService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Address;
import com.sd20201.datn.entity.Customer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AdAddressRepository addressRepository;

    @Override
    public ResponseObject<?> getAllAddresses(AddressRequest request) {
        Page<AddressResponse> page = addressRepository.getAllAddresses(
                PageRequest.of(request.getPage(), request.getSize()),
                request.getCustomerId(),
                request.getProvinceCity()
        );
        return new ResponseObject<>(
                PageableObject.of(page),
                HttpStatus.OK,
                "Lấy danh sách địa chỉ thành công"
        );
    }

    @Override
    public ResponseObject<?> getAddressesByCustomer(String customerId) {
        List<Address> addresses = addressRepository.findByCustomerId(customerId);
        return new ResponseObject<>(
                addresses,
                HttpStatus.OK,
                "Lấy tất cả địa chỉ thành công"
        );
    }

    @Override
    public ResponseObject<?> getDefaultAddress(String customerId) {
        Address address = addressRepository.findByCustomerIdAndIsDefaultTrue(customerId).orElse(null);
        return new ResponseObject<>(
                address,
                HttpStatus.OK,
                "Lấy địa chỉ mặc định thành công"
        );
    }

    @Override
    public ResponseObject<?> addAddress(String customerId, AddressCreateUpdateRequest request) {
        Address address = new Address();
        address.setProvinceCity(request.getProvinceCity());
        address.setDistrict(request.getDistrict());
        address.setWardCommune(request.getWardCommune());
        address.setAddressDetail(request.getAddressDetail());

        Customer customer = new Customer();
        customer.setId(customerId);
        address.setCustomer(customer);

        // Nếu là địa chỉ đầu tiên thì luôn mặc định
        if (addressRepository.findByCustomerId(customerId).isEmpty()) {
            address.setIsDefault(true);
        } else if (Boolean.TRUE.equals(request.getIsDefault())) {
            // Nếu request muốn set mặc định -> clear các địa chỉ khác
            List<Address> addresses = addressRepository.findByCustomerId(customerId);
            for (Address a : addresses) {
                a.setIsDefault(false);
            }
            addressRepository.saveAll(addresses);
            address.setIsDefault(true);
        } else {
            address.setIsDefault(false);
        }

        Address saved = addressRepository.save(address);
        return new ResponseObject<>(
                saved,
                HttpStatus.OK,
                "Thêm địa chỉ thành công"
        );
    }

    @Override
    public ResponseObject<?> updateAddress(String addressId, AddressCreateUpdateRequest request) {
        Address existing = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        existing.setProvinceCity(request.getProvinceCity());
        existing.setDistrict(request.getDistrict());
        existing.setWardCommune(request.getWardCommune());
        existing.setAddressDetail(request.getAddressDetail());

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            List<Address> addresses = addressRepository.findByCustomerId(existing.getCustomer().getId());
            for (Address a : addresses) {
                if (!a.getId().equals(addressId)) {
                    a.setIsDefault(false);
                }
            }
            addressRepository.saveAll(addresses);
            existing.setIsDefault(true);
        }

        Address updated = addressRepository.save(existing);
        return new ResponseObject<>(
                updated,
                HttpStatus.OK,
                "Cập nhật địa chỉ thành công"
        );
    }


    @Override
    public ResponseObject<?> deleteAddress(String addressId) {
        addressRepository.deleteById(addressId);
        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Xóa địa chỉ thành công"
        );
    }

    @Override
    @Transactional
    public ResponseObject<?> setDefaultAddress(String customerId, String addressId) {
        List<Address> addresses = addressRepository.findByCustomerId(customerId);
        for (Address a : addresses) {
            a.setIsDefault(a.getId().equals(addressId));
        }
        addressRepository.saveAll(addresses);
        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Cập nhật địa chỉ mặc định thành công"
        );
    }
}
