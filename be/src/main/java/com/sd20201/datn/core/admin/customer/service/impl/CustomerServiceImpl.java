package com.sd20201.datn.core.admin.customer.service.impl;

import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.core.admin.customer.model.request.CustomerCreateUpdateRequest;
import com.sd20201.datn.core.admin.customer.model.request.CustomerRequest;
import com.sd20201.datn.core.admin.customer.repository.AdCustomerRepository;
import com.sd20201.datn.core.admin.customer.service.CustomerService;
import com.sd20201.datn.entity.Account;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.repository.AccountRepository;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final AdCustomerRepository adCustomerRepository;
    private final AccountRepository accountRepository;

    @Override
    public ResponseObject<?> getAllCustomers(CustomerRequest request) {
        Pageable pageable = Helper.createPageable(request, "createdDate");

        return new ResponseObject<>(
                PageableObject.of(adCustomerRepository.getAllCustomers(
                        pageable,
                        request.getCustomerName(),
                        request.getCustomerStatus(),
                        request.getCustomerGender()
                )),
                HttpStatus.OK,
                "Lấy thành công danh sách khách hàng"
        );
    }

    @Override
    public ResponseObject<?> createCustomer(CustomerCreateUpdateRequest request) {
        // Kiểm tra tên trùng
        List<Customer> customersByName = adCustomerRepository.findByExactName(request.getCustomerName());
        if (!customersByName.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                    "Tên khách hàng đã tồn tại", false, "CUSTOMER_NAME_EXISTS");
        }

        // Kiểm tra phone trùng
        List<Customer> customersByPhone = adCustomerRepository.findByExactPhone(request.getCustomerPhone());
        if (!customersByPhone.isEmpty()) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                    "Số điện thoại đã tồn tại", false, "CUSTOMER_PHONE_EXISTS");
        }

        // Xử lý account
        Account account = null;
        if (request.getCustomerIdAccount() != null) {
            account = accountRepository.findById(request.getCustomerIdAccount()).orElse(null);
            if (account == null) {
                return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                        "Tài khoản không tồn tại", false, "ACCOUNT_NOT_FOUND");
            }
        }

        // Tạo customer
        Customer customer = new Customer();
        customer.setAccount(account);
        customer.setName(request.getCustomerName());
        customer.setPhone(request.getCustomerPhone());
        customer.setEmail(request.getCustomerEmail());
        customer.setAvatarUrl(request.getCustomerAvatar());
        customer.setBirthday(request.getCustomerBirthday());
        customer.setGender(request.getCustomerGender());
        customer.setDescription(request.getCustomerDescription());

        // ✅ Mapping status
        if (request.getCustomerStatus() != null) {
            EntityStatus status;
            switch (request.getCustomerStatus()) {
                case 1:
                    status = EntityStatus.ACTIVE;
                    break;
                case 0:
                    status = EntityStatus.INACTIVE;
                    break;
                default:
                    status = EntityStatus.ACTIVE;
                    break;
            }
            customer.setStatus(status);
        } else {
            customer.setStatus(EntityStatus.ACTIVE); // Default
        }
        customer.setCreatedDate(System.currentTimeMillis());

// Thêm debug này NGAY SAU khi save:
        adCustomerRepository.save(customer);
        adCustomerRepository.flush(); // Force DB write

// Query lại từ DB
        Customer freshCustomer = adCustomerRepository.findById(customer.getId()).orElse(null);

// ✅ Response data - SỬA LOGIC NGƯỢC
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("id", customer.getId());
        responseData.put("customerName", customer.getName());
        responseData.put("customerPhone", customer.getPhone());
        responseData.put("customerEmail", customer.getEmail());
        responseData.put("customerGender", customer.getGender());
        responseData.put("customerBirthday", customer.getBirthday());

// Dùng fresh data cho response
        int actualResponseStatus = freshCustomer.getStatus() == EntityStatus.ACTIVE ? 1 : 0;
        responseData.put("customerStatus", actualResponseStatus);
        responseData.put("customerCode", customer.getCode());
        responseData.put("customerAvatar", customer.getAvatarUrl());
        responseData.put("customerDescription", customer.getDescription());
        responseData.put("customerCreatedDate", customer.getCreatedDate());
        return new ResponseObject<>(responseData, HttpStatus.OK, "Thêm khách hàng thành công", true, null);
    }

    @Override
    public ResponseObject<?> updateCustomer(String id, CustomerCreateUpdateRequest request) {
        Customer customer = adCustomerRepository.findById(id).orElse(null);
        if (customer == null) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy khách hàng với id = " + id,
                    false,
                    "CUSTOMER_NOT_FOUND"
            );
        }

        // ✅ Kiểm tra trùng tên (ngoại trừ chính mình)
        List<Customer> customersByName = adCustomerRepository.findByExactName(request.getCustomerName());
        if (!customersByName.isEmpty() && !customersByName.get(0).getId().equals(id)) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Tên khách hàng đã tồn tại",
                    false,
                    "CUSTOMER_NAME_EXISTS"
            );
        }

        // ✅ Kiểm tra trùng số điện thoại (ngoại trừ chính mình)
        List<Customer> customersByPhone = adCustomerRepository.findByExactPhone(request.getCustomerPhone());
        if (!customersByPhone.isEmpty() && !customersByPhone.get(0).getId().equals(id)) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.BAD_REQUEST,
                    "Số điện thoại đã tồn tại",
                    false,
                    "CUSTOMER_PHONE_EXISTS"
            );
        }

        // Xử lý account nếu có customerIdAccount
        if (request.getCustomerIdAccount() != null) {
            Account account = accountRepository.findById(request.getCustomerIdAccount()).orElse(null);
            if (account == null) {
                return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                        "Tài khoản không tồn tại", false, "ACCOUNT_NOT_FOUND");
            }
            customer.setAccount(account);
        } else {
            customer.setAccount(null);
        }

        // Cập nhật thông tin khác
        customer.setName(request.getCustomerName());
        customer.setPhone(request.getCustomerPhone());
        customer.setEmail(request.getCustomerEmail());
        if (request.getCustomerAvatar() != null && !request.getCustomerAvatar().trim().isEmpty()) {
            customer.setAvatarUrl(request.getCustomerAvatar().trim());
        } else if (request.getCustomerAvatar() != null && request.getCustomerAvatar().trim().isEmpty()) {
            customer.setAvatarUrl(null); // Clear avatar nếu gửi empty string
        }
        customer.setBirthday(request.getCustomerBirthday());
        customer.setGender(request.getCustomerGender());
        customer.setDescription(request.getCustomerDescription());

        // 🔥 THÊM DÒNG NÀY - Cập nhật status
        if (request.getCustomerStatus() != null) {
            EntityStatus status = request.getCustomerStatus() == 1
                    ? EntityStatus.ACTIVE
                    : EntityStatus.INACTIVE;
            customer.setStatus(status);
        }

        adCustomerRepository.save(customer);

        return new ResponseObject<>(
                customer,
                HttpStatus.OK,
                "Cập nhật khách hàng thành công",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> deleteCustomer(String id) {
        Customer customer = adCustomerRepository.findById(id).orElse(null);
        if (customer == null) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy khách hàng với id = " + id,
                    false,
                    "CUSTOMER_NOT_FOUND"
            );
        }

        // 🔹 Xóa mềm
        customer.setStatus(EntityStatus.INACTIVE);
        adCustomerRepository.save(customer);

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Xóa khách hàng thành công",
                true,
                null
        );
    }
}
