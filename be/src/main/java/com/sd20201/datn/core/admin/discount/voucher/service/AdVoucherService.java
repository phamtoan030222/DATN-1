package com.sd20201.datn.core.admin.discount.voucher.service;

import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Voucher;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AdVoucherService {
    ResponseObject<?> getVouchers(AdVoucherRequest request);

    ResponseObject<?> getVoucherById(String id);

    ResponseObject<?> changeStatusVoucher(String id);

    @Transactional
    ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request) throws BadRequestException;

    @Transactional
    ResponseObject<?> update(@PathVariable String id, @Valid AdVoucherCreateUpdateRequest request) throws BadRequestException;

    ResponseObject<?> deleteById(String id) ;

    ResponseObject<?> deleteAllByIds(List<String> ids) ;

    Map<Voucher, List<Customer>> getCustomersByVoucher(boolean onlyActive);

    Map<Customer, List<Voucher>> getVouchersByCustomer(boolean onlyActive);
}

