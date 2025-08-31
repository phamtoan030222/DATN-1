package com.sd20201.datn.core.admin.discount.voucher.service;

import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;

public interface AdVoucherService {
    ResponseObject<?> getVouchers(AdVoucherRequest request);

    ResponseObject<?> getVoucherById(String id);

    ResponseObject<?> changeStatusVoucher(String id);

    @Transactional
    ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request) throws BadRequestException;

    @Transactional
    ResponseObject<?> update(@PathVariable String id, @Valid AdVoucherCreateUpdateRequest request) throws BadRequestException;

    public ResponseObject<?> deleteById(String id) ;

    public ResponseObject<?> deleteAllByIds(List<String> ids) ;

    public ResponseObject<?> claimVoucher(String voucherId, String customerId, BigDecimal currentOrderValue);
}
