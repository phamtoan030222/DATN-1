package com.sd20201.datn.core.admin.voucher.voucher.service;

import com.sd20201.datn.core.admin.voucher.voucher.model.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.admin.voucher.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Voucher;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AdVoucherService {

    /* Danh sách + chi tiết voucher */
    ResponseObject<?> getVouchers(AdVoucherRequest request);

    ResponseObject<?> getVoucherById(String id);

    /* Đổi trạng thái ACTIVE/INACTIVE */
    ResponseObject<?> changeStatusVoucherToStart(String id);

    ResponseObject<?> changeStatusVoucherToEnd(String id);

    ResponseObject<?> changeStatusVoucher(String id);


    /* Tạo / cập nhật / xóa */
    ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request) throws BadRequestException;

    ResponseObject<?> update(String id, @Valid AdVoucherCreateUpdateRequest request) throws BadRequestException;

    ResponseObject<?> deleteById(String id);

    ResponseObject<?> deleteAllByIds(List<String> ids);

    /* Bản đồ quan hệ Voucher <-> Customer (không phân trang) */
    Map<Voucher, List<Customer>> getCustomersByVoucher(boolean onlyActive);

    Map<Customer, List<Voucher>> getVouchersByCustomer(boolean onlyActive);


    ResponseObject<?> getCustomersOfVoucher(String voucherId, boolean onlyUsed, Pageable pageable);
}
