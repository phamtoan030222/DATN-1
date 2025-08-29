package com.sd20201.datn.core.admin.discount.voucher.service;

import com.sd20201.datn.core.admin.discount.voucher.motel.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.admin.discount.voucher.motel.request.AdVoucherRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface AdVoucherService {
    ResponseObject<?> getVouchers(AdVoucherRequest request);

    ResponseObject<?> getVoucherById(String id);

    ResponseObject<?> changeStatusVoucher(String id);

    @Transactional
    ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request);

    @Transactional
    ResponseObject<?> update(@PathVariable String id, @Valid AdVoucherCreateUpdateRequest request);

    public void distributeToCustomers(String voucherId, List<String> userIds);
}
