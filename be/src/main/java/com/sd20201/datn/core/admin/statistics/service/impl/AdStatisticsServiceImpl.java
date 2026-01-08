package com.sd20201.datn.core.admin.statistics.service.impl;

import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdProductRespone;
import com.sd20201.datn.core.admin.statistics.model.response.AdInvoiceResponse;
import com.sd20201.datn.core.admin.statistics.repository.AdInvoiceRepository;
import com.sd20201.datn.core.admin.statistics.service.AdStatisticsService;
import com.sd20201.datn.core.common.base.ResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AdStatisticsServiceImpl implements AdStatisticsService {

    @Autowired
    private AdInvoiceRepository adInvoiceRepository;

    @Override
    public ResponseObject<Page<AdProductRespone>> getAllProducts(Pageable pageable) {
        return null;
    }

    @Override
    public ResponseObject<Page<AdInvoiceResponse>> getAllInvoice(Pageable pageable) {
        Page<AdInvoiceResponse> page = adInvoiceRepository.getAllInvoice(pageable);

        return new ResponseObject<>(
                page,
                HttpStatus.OK,
                "Lấy danh sách hoá đơn thành công. Tổng số sản phẩm: " + page.getTotalElements(),
                true,
                null
        );
    }
}
