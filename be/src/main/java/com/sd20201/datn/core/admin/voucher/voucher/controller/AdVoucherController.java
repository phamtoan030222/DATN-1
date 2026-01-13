package com.sd20201.datn.core.admin.voucher.voucher.controller;

import com.sd20201.datn.core.admin.voucher.voucher.model.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.admin.voucher.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.admin.voucher.voucher.repository.AdVoucherRepository;
import com.sd20201.datn.core.admin.voucher.voucher.service.AdVoucherService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.utils.Helper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_DISCOUNT_VOUCHER)
@RequiredArgsConstructor
public class AdVoucherController {
    private final AdVoucherService voucherService;
    private final AdVoucherRepository voucherRepository;

    @GetMapping
    ResponseEntity<?> getAllVouchers(@ModelAttribute AdVoucherRequest request) {
        return Helper.createResponseEntity(voucherService.getVouchers(request));
    }

    @GetMapping("/{id}")
    ResponseEntity<?> getVoucher(@PathVariable String id) {
        return Helper.createResponseEntity(voucherService.getVoucherById(id));
    }

    @PatchMapping("/{id}/status")
    ResponseEntity<?> changeStatus(@PathVariable String id) {
        return Helper.createResponseEntity(voucherService.changeStatusVoucher(id));
    }

    @PostMapping()
    public ResponseEntity<?> createVoucher(@Valid @RequestBody AdVoucherCreateUpdateRequest request) throws BadRequestException {
        return Helper.createResponseEntity(voucherService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVoucher(@PathVariable String id,@Valid @RequestBody AdVoucherCreateUpdateRequest request) throws BadRequestException {
        return Helper.createResponseEntity(voucherService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOne(@PathVariable String id) {
        return Helper.createResponseEntity(voucherService.deleteById(id));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteMany(@RequestBody List<String> ids) {
        return Helper.createResponseEntity(voucherService.deleteAllByIds(ids));
    }

    @GetMapping("/customers-by-voucher")
    public ResponseEntity<?> getCustomersByVoucher(@RequestParam(defaultValue = "false") boolean onlyActive) {
        try {
            Map<Voucher, List<Customer>> result = voucherService.getCustomersByVoucher(onlyActive);
            if (result.isEmpty()) {
                return Helper.createResponseEntity(ResponseObject.errorForward("Không tìm thấy dữ liệu", HttpStatus.NOT_FOUND));
            }
            return Helper.createResponseEntity(ResponseObject.successForward(result, "Thành công"));
        } catch (Exception e) {
            return Helper.createResponseEntity(ResponseObject.errorForward("Lỗi: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/{id}/customers")
    public ResponseEntity<?> customersByVoucher(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean onlyUsed,
            @ModelAttribute AdVoucherRequest pageReq // Đã đổi từ PageableRequest -> AdVoucherRequest
    ) {
        var pageable = Helper.createPageable(pageReq, "createdDate");
        return Helper.createResponseEntity(
                voucherService.getCustomersOfVoucher(id, onlyUsed, pageable)
        );
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<?> startVoucher(@PathVariable String id) {
        return Helper.createResponseEntity(voucherService.changeStatusVoucherToStart(id));
    }

    @PatchMapping("/{id}/end")
    public ResponseEntity<?> endVoucher(@PathVariable String id) {
        return Helper.createResponseEntity(voucherService.changeStatusVoucherToEnd(id));
    }

}