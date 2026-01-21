package com.sd20201.datn.core.admin.customer.controller;

import com.sd20201.datn.core.admin.customer.model.request.CustomerCreateUpdateRequest;
import com.sd20201.datn.core.admin.customer.model.request.CustomerRequest;
import com.sd20201.datn.core.admin.customer.model.response.CustomerResponse;
import com.sd20201.datn.core.admin.customer.repository.AdCustomerRepository;
import com.sd20201.datn.core.admin.customer.service.CustomerService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_CUSTOMERS)
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    private final AdCustomerRepository adCustomerRepository;

    @GetMapping
    public ResponseEntity<?> getAllCustomers(@ModelAttribute CustomerRequest request) {
        return Helper.createResponseEntity(customerService.getAllCustomers(request));
    }

    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody CustomerCreateUpdateRequest request) {
        return Helper.createResponseEntity(customerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable String id,
            @RequestBody CustomerCreateUpdateRequest request
    ) {
        return Helper.createResponseEntity(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable String id) {
        return Helper.createResponseEntity(customerService.deleteCustomer(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(@PathVariable String id) {
        return Helper.createResponseEntity(customerService.getCustomerById(id));
    }

    @GetMapping("/filter-with-stats")
    public ResponseEntity<?> getCustomersWithStats(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "timeRange", required = false, defaultValue = "YEAR") String timeRange, // MONTH hoặc YEAR
            @RequestParam(name = "sortField", required = false) String sortField, // totalOrders hoặc totalSpending
            @RequestParam(name = "sortDirection", required = false) String sortDirection // asc hoặc desc
    ) {
        // SỬA LỖI: Bỏ ép kiểu (ResponseObject<?>).
        // Helper.createResponseEntity sẽ tự động đóng gói Page<CustomerResponse> vào ResponseObject.
        return Helper.createResponseEntity((customerService.getCustomersWithStats(page, size, keyword, timeRange, sortField, sortDirection)));
    }
}
