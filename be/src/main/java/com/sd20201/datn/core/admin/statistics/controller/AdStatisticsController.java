package com.sd20201.datn.core.admin.statistics.controller;

import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdProductRespone;
import com.sd20201.datn.core.admin.statistics.model.response.AdInvoiceResponse;
import com.sd20201.datn.core.admin.statistics.service.impl.AdStatisticsServiceImpl;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.infrastructure.constant.MappingConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MappingConstants.API_ADMIN_PREFIX_STATISTICS)
@RequiredArgsConstructor
public class AdStatisticsController {

    @Autowired
    private AdStatisticsServiceImpl adStatisticsService;


    @GetMapping
    public ResponseObject<Page<AdProductRespone>> getAllHotProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return adStatisticsService.getAllProducts(PageRequest.of(page, size));
    }

    @GetMapping("/invoice")
    public ResponseObject<Page<AdInvoiceResponse>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return adStatisticsService.getAllInvoice(PageRequest.of(page, size));
    }

}
